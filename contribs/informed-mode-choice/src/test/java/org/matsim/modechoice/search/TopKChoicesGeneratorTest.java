package org.matsim.modechoice.search;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TopKChoicesGeneratorTest extends ScenarioTest {

	@Test
	public void choices() {

		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan());

		assertThat(candidates)
				.hasSize(5)
				.first()
				.isEqualTo(new PlanCandidate(new String[]{"car", "car", "car", "car"}, Double.NaN));


		person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(1));
		candidates = generator.generate(person.getSelectedPlan());
		assertThat(candidates)
				.hasSize(5)
				.first()
				.isEqualTo(new PlanCandidate(new String[]{"car", "car", "car", "car", "car", "car", "car"}, Double.NaN));

	}

	@Test
	public void person() {

		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(Id.createPersonId("10390"));

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan());

		assertThat(candidates)
				.contains(new PlanCandidate(new String[]{"car"}, -5.96));

	}


	@Test
	public void invariance() {

		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);


		for (Person p : controler.getScenario().getPopulation().getPersons().values()) {

			if (!PersonUtils.canUseCar(p))
				continue;

			Plan orig = p.getSelectedPlan();

			Plan car = p.createCopyOfSelectedPlanAndMakeSelected();
			setLegs(car, "car");
			Collection<PlanCandidate> carCandidates = generator.generate(car);

			p.setSelectedPlan(orig);

			Plan walk = p.createCopyOfSelectedPlanAndMakeSelected();
			setLegs(walk, "walk");
			Collection<PlanCandidate> walkCandidates = generator.generate(walk);

			assertThat(carCandidates)
					.isEqualTo(walkCandidates);
		}
	}

	@Test
	public void predefined() {

		TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

		TopKChoicesGenerator.Context ctx = generator.generate(person.getSelectedPlan(), null, 6, 0);

		PlanCandidate first = ctx.getResult().iterator().next();

		assertThat(first.getPlanType()).isEqualTo("car-car-car-car");
		assertThat(first.getUtility()).isEqualTo(-6.186329864145045);


		List<PlanCandidate> candidates = generator.generatePredefined(ctx, List.of(
				new String[]{"car", "car", "car", "car"},
				new String[]{"walk", "walk", "walk", "walk"},
				new String[]{"walk", "bike", null, "car"}
		));

		PlanCandidate car = candidates.get(0);

		assertThat(car.getPlanType()).isEqualTo("car-car-car-car");
		assertThat(car.getUtility()).isEqualTo(first.getUtility());

		PlanCandidate walk = candidates.get(1);

		assertThat(walk.getPlanType()).isEqualTo("walk-walk-walk-walk");
		assertThat(walk.getUtility()).isLessThan(first.getUtility());

	}

	/**
	 * Normalize modes for all legs.
	 */
	private static void setLegs(Plan plan, String mode) {
		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
			for (Leg leg : trip.getLegsOnly()) {
				leg.setRoute(null);
				leg.setMode(mode);
				TripStructureUtils.setRoutingMode(leg, mode);
			}
		}
	}

}