package org.matsim.modechoice;

import org.apache.commons.lang3.StringUtils;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripStructureUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Candidate for trip modes. One candidate is compared to others by the minimum utility estimate.
 */
public final class PlanCandidate implements Comparable<PlanCandidate> {

	public static final String ESTIMATE_ATTR = "estimate";
	public static final String TYPE_ATTR = "modeType";

	private final String[] modes;

	/**
	 * Estimated maximum utility. An estimate should never underestimate the utility.
	 */
	private final double utility;

	public PlanCandidate(String[] modes, double utility) {
		this.modes = modes;
		this.utility = utility;
	}

	/**
	 * Total estimated utility.
	 */
	public double getUtility() {
		return utility;
	}

	/**
	 * Get mode for trip i.
	 */
	public String getMode(int i) {
		return modes[i];
	}

	/**
	 * Return a copy of this candidate's modes.
	 */
	public String[] getModes() {
		return Arrays.copyOf(modes, modes.length);
	}

	/**
	 * Return features vector with number of occurrences per mode.
	 */
	public static double[] occurrences(List<String> modes, String type) {

		double[] ft = new double[modes.size()];

		for (int i = 0; i < modes.size(); i++) {
			int count = StringUtils.countMatches(type, modes.get(i));
			ft[i] = count;
		}

		return ft;
	}

	/**
	 * Applies the routing modes of this candidate to a plan.
	 *
	 * @param plan
	 */
	public void applyTo(Plan plan) {

		plan.setScore(null);

		applyAttributes(plan);

		int k = 0;
		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {

			// increment k after only usage here
			String mode = modes[k++];

			// modes could be not specified
			if (mode == null)
				continue;

			for (Leg leg : trip.getLegsOnly()) {

				leg.setRoute(null);
				leg.setMode(mode);
				TripStructureUtils.setRoutingMode(leg, mode);
			}
		}
	}

	/**
	 * Set estimates as plan attributes.
	 */
	public void applyAttributes(Plan plan) {

		plan.getAttributes().putAttribute(TYPE_ATTR, getPlanType());
		plan.getAttributes().putAttribute(ESTIMATE_ATTR, utility);

	}

	/**
	 * Return identifier for chosen modes.
	 */
	public String getPlanType() {

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < modes.length; i++) {

			b.append(modes[i]);
			if (i != modes.length - 1)
				b.append("-");
		}

		return b.toString();
	}

	/**
	 * Infer plan type from routing modes.
	 */
	public static String guessPlanType(Plan plan, List<String> modes) {

		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < trips.size(); i++) {

			String routingMode = TripStructureUtils.getRoutingMode(trips.get(i).getLegsOnly().get(0));
			if (!modes.contains(routingMode))
				routingMode = null;

			b.append(routingMode);
			if (i != trips.size() - 1)
				b.append("-");
		}

		return b.toString();
	}

	/**
	 * Creates array of selected modes from plan type.
	 */
	public static String[] createModeArray(String planType) {
		String[] modes = planType.split("-");
		for (int i = 0; i < modes.length; i++) {

			if (modes[i].equals("null"))
				modes[i] = null;
			else
				modes[i] = modes[i].intern();
		}

		return modes;
	}

	@Override
	public int compareTo(PlanCandidate o) {
		return Double.compare(o.utility, utility);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PlanCandidate that = (PlanCandidate) o;
		return Arrays.equals(modes, that.modes);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(modes);
	}


	@Override
	public String toString() {
		return "PlanCandidate{" +
				"modes=" + Arrays.toString(modes) +
				", utility=" + utility + '}';
	}
}
