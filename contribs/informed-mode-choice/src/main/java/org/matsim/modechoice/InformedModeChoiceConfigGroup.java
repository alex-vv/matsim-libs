package org.matsim.modechoice;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Config group for informed mode choice. Most options need to be configured via the builder.
 */
public class InformedModeChoiceConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "informedModeChoice";

	public final static String CONFIG_PARAM_MODES = "modes";
	public final static String CONFIG_PARAM_TOP_K = "topK";
	public final static String CONFIG_PARAM_INV_BETA = "invBeta";

	public final static String CONFIG_PARAM_ANNEAL = "anneal";


	/**
	 * The setter ensures, that this class always contains internal string representations.
	 */
	private List<String> modes = List.of(TransportMode.car, TransportMode.walk, TransportMode.pt, TransportMode.bike);

	/**
	 * Use kth best trips.
	 */
	private int k = 5;

	/**
	 * Scale parameter for MNL.
	 */
	private double invBeta = 1;

	/**
	 * Annealing schedule.
	 */
	private Schedule anneal = Schedule.off;

	public InformedModeChoiceConfigGroup() {
		super(NAME);
	}

	@StringSetter(CONFIG_PARAM_MODES)
	private void setModes(final String value) {
		setModes(Splitter.on(",").split(value));
	}

	@StringGetter(CONFIG_PARAM_MODES)
	private String getStringModes() {
		return Joiner.on(",").join(modes);
	}

	public void setModes(Iterable<String> modes) {
		this.modes = StreamSupport.stream(modes.spliterator(), false)
				.map(String::intern)
				.distinct()
				.collect(Collectors.toList());
	}

	@StringSetter(CONFIG_PARAM_TOP_K)
	public void setTopK(int k) {
		this.k = k;
	}

	@StringGetter(CONFIG_PARAM_TOP_K)
	public int getTopK() {
		return k;
	}

	@StringGetter(CONFIG_PARAM_INV_BETA)
	public double getInvBeta() {
		return invBeta;
	}

	@StringSetter(CONFIG_PARAM_INV_BETA)
	public void setInvBeta(double invBeta) {
		this.invBeta = invBeta;
	}

	@StringGetter(CONFIG_PARAM_ANNEAL)
	public Schedule getAnneal() {
		return anneal;
	}

	@StringSetter(CONFIG_PARAM_ANNEAL)
	public void setAnneal(Schedule anneal) {
		this.anneal = anneal;
	}

	public List<String> getModes() {
		return modes;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(CONFIG_PARAM_MODES, "Defines all modes that are available and open for mode choice.");
		comments.put(CONFIG_PARAM_TOP_K, "Defines how many top k best trips of each category should be generated.");
		comments.put(CONFIG_PARAM_INV_BETA, "1/beta parameter to trade-off of exploration for alternatives. Parameter of 0 is equal to best choice.");
		comments.put(CONFIG_PARAM_ANNEAL, "Annealing for the invBeta parameter.");

		return comments;
	}

	public enum Schedule {
		off,
		linear
	}
}
