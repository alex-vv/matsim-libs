package org.matsim.modechoice.commands;


import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;
import picocli.CommandLine;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "generate-choice-set",
		description = "Generate a static mode-choice set for all agents in the population"
)
public class GenerateChoiceSet implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(GenerateChoiceSet.class);

	@CommandLine.Option(names = "--config", description = "Path to scenario config", required = true)
	private Path configPath;

	@CommandLine.Option(names = "--scenario", description = "Full qualified classname of the MATSim application scenario class. The IMC modules must be specified there.", required = true)
	private Class<? extends MATSimApplication> scenario;

	@CommandLine.Option(names = "--args", description = "Arguments passed to the scenario")
	private String scenarioArgs;

	@CommandLine.Option(names = "--population", description = "Path to input population")
	private Path populationPath;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--top-k", description = "Use top k estimates", defaultValue = "5")
	private int topK;

	@CommandLine.Option(names = "--threshold", description = "Cut-off plans below certain score scaled by distance.", defaultValue = "0")
	private double threshold;

	@CommandLine.Option(names = "--modes", description = "Modes to include in estimation", defaultValue = "car,walk,bike,pt,ride", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--output-dist", description = "Write estimation distribution to output. Filename is derived from output", defaultValue = "false")
	private boolean outputDist;

	private Writer distWriter;

	private ThreadLocal<TopKChoicesGenerator> generatorCache;

	public static void main(String[] args) {
		new GenerateChoiceSet().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = ConfigUtils.loadConfig(configPath.toString());

		if (populationPath != null)
			config.plans().setInputFile(populationPath.toString());

		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		log.info("Using k={}, threshold={}", topK, threshold);

		imc.setTopK(topK);
		imc.setModes(modes);

		Controler controler;

		if (scenarioArgs == null || scenarioArgs.isBlank())
			controler = MATSimApplication.prepare(scenario, config);
		else
			controler = MATSimApplication.prepare(scenario, config, scenarioArgs);

		Injector injector = controler.getInjector();

		generatorCache = ThreadLocal.withInitial(() -> injector.getInstance(TopKChoicesGenerator.class));

		// copy the original plan, so no modifications are made
		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			String subpop = PopulationUtils.getSubpopulation(person);
			if (subpopulation != null && !subpop.equals(subpopulation))
				continue;

			Plan selected = person.getSelectedPlan();
			selected.setScore(null);

			Plan copy = person.createCopyOfSelectedPlanAndMakeSelected();
			copy.setType("source");

			person.setSelectedPlan(selected);
		}


		// THis is currently needed because vehicle id mapping needs to be initialized
		controler.run();

		injector.injectMembers(this);

		log.info("Estimating choice set...");

		if (outputDist) {

			String name = output.getFileName().toString().replace(".gz", "").replace(".xml", ".tsv");
			Path out = output.getParent().resolve(name);

			log.info("Writing output distribution to {}", out);

			distWriter = Files.newBufferedWriter(out);
			distWriter.write("person\testimates\n");
		}

		ParallelPersonAlgorithmUtils.run(controler.getScenario().getPopulation(), config.global().getNumberOfThreads(), this);

		PopulationUtils.writePopulation(controler.getScenario().getPopulation(), output.toString());

		if (distWriter != null)
			distWriter.close();

		return 0;
	}

	@Override
	public void run(Person person) {

		String subpop = PopulationUtils.getSubpopulation(person);
		if (subpopulation != null && !subpop.equals(subpopulation))
			return;

		Plan plan = person.getPlans().stream().filter(p -> "source".equals(p.getType())).findFirst().orElseThrow();

		double threshold = this.threshold;

		// the absolute threshold is scaled to distance
		if (this.threshold > 0) {
			PlanModel model = new PlanModel(plan);
			threshold = model.distance() * this.threshold;
		}

		TopKChoicesGenerator generator = generatorCache.get();
		Collection<PlanCandidate> candidates = generator.generate(plan, null, topK, threshold).getResult();

		// remove all other plans
		Set<Plan> plans = new HashSet<>(person.getPlans());
		plans.remove(plan);
		plans.forEach(person::removePlan);

		for (PlanCandidate c : candidates) {

			if (plan == null)
				plan = person.createCopyOfSelectedPlanAndMakeSelected();

			c.applyTo(plan);
			plan.setType(c.getPlanType());
			plan = null;
		}

		if (distWriter != null) {

			// the writer is synchronized
			try {
				distWriter.write(person.getId() + "\t" +
						candidates.stream()
								.map(PlanCandidate::getUtility).map(String::valueOf)
								.collect(Collectors.joining(";")) +
						"\n");

			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

	}
}
