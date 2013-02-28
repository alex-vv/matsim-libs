package playground.mzilske.vbb;

import java.io.File;
import java.io.IOException;

import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;

final class OTPTripRouterFactory implements
		TripRouterFactory {
	
	
	private Graph graph;

	

	public OTPTripRouterFactory(TransitSchedule transitSchedule) {
		File path = new File("/tmp/graph-bundle/Graph.obj");
		try {
			graph = Graph.load(path, Graph.LoadLevel.DEBUG);
		} catch (IOException e) {
			throw new RuntimeException();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException();
		}
		pathservice.setGraphService(graphservice);
		pathservice.setSptService(sptService);
		this.transitSchedule = transitSchedule;
	}

	private GraphServiceImpl graphservice = new GraphServiceImpl() {
		public Graph getGraph(String routerId) { return graph; }
	};

	private RetryingPathServiceImpl pathservice = new RetryingPathServiceImpl();

	private GenericAStar sptService = new GenericAStar();

	private TransitSchedule transitSchedule;
	
	@Override
	public TripRouter createTripRouter() {
		TripRouter tripRouter = new TripRouter();
		tripRouter.setRoutingModule("pt", new OTPRoutingModule(pathservice, transitSchedule));
		return tripRouter;
	}
}