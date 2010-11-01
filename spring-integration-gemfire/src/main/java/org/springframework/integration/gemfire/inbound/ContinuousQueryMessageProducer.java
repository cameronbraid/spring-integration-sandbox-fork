package org.springframework.integration.gemfire.inbound;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionService;
import com.gemstone.gemfire.cache.query.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Responds to a continuous query (set using the #queryString field) that is
 * constantly evaluated against a cache {@link com.gemstone.gemfire.cache.Region}.
 * This is much faster than re-querying the cache manually.
 *
 * @author Josh Long
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ContinuousQueryMessageProducer extends MessageProducerSupport {


	private final Log logger = LogFactory.getLog(this.getClass());

	/**
	 * Must be provided by the client of this class
	 */
	private final Region<?, ?> region;

	/**
	 * Is the queryString durable (optional)
	 */
	private volatile boolean durable = false;

	/**
	 * the {@link com.gemstone.gemfire.cache.query.CqQuery} instance created and registerd with the server
	 */
	private volatile CqQuery cqQuery;

	/**
	 * the query to be registered against the cache
	 */
	private final String queryString;

	/**
	 * a reference to the {@link com.gemstone.gemfire.cache.RegionService} that is obtained through the region
	 */
	private volatile RegionService regionService;

	/**
	 * a reference to a {@link com.gemstone.gemfire.cache.query.QueryService} that is obtained through the #regionService instance.
	 */
	private volatile QueryService queryService;

	/**
	 * used when building the queryString itself - optional
	 */
	private volatile String queryName;

	/**
	 * {@link com.gemstone.gemfire.cache.query.CqAttributes} attributes as factoried
	 * from a {@link com.gemstone.gemfire.cache.query.CqAttributesFactory}
	 */
	private volatile CqAttributes cqAttributes;

	/**
	 * a {@link com.gemstone.gemfire.cache.query.CqAttributesFactory} to
	 * factory the {@link com.gemstone.gemfire.cache.query.CqAttributes} that in turn hold the refernece to the
	 * listener that we register to in turn funnel messages to the clients of this adapter
	 */
	private volatile CqAttributesFactory cqAttributesFactory;

	/**
	 * the adapter requires a query string to continuously evaluate as well as a
	 * {@link com.gemstone.gemfire.cache.Region} against which to evaluate the query.
	 *
	 *
	 * @param region the region against which the query should be evaluated
	 * @param queryString the query string - see 
	 */
	public ContinuousQueryMessageProducer(Region<?, ?> region, String queryString) {
		this.region = region;
		Assert.notNull(this.region, "You must provide a reference to a 'Region'");

		this.queryString = queryString;
		Assert.hasText(this.queryString, "You must provide a queryString to evaluate against the region");
	}

	/**
	 * whether or not the query is durable (that is, whether or not this query should live beyond the registered query)
	 * @param durable whether or not the query is registered and saved and subsequently retreivable by a query name.
	 *
	 */
	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	/**
	 * the name of the queryString (optional)
	 *
	 * @param queryName the query name
	 */
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	@Override
	protected void doStart() {
		try {
			cqQuery.execute();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			this.cqQuery.stop();
		} catch (CqException e) {
			throw new RuntimeException("Can't stop the continuous query", e);
		}
	}

	@Override
	protected void onInit() {
		try {
			super.onInit();

			regionService = this.region.getRegionService();
			queryService = this.region.getRegionService().getQueryService();

			String defaultName = String.format("%s-%s-query", getComponentName() + "", getComponentType() + "");

			queryName = StringUtils.hasText(queryName) ? queryName : defaultName;

			// lets first see if the query already exists
			cqAttributesFactory = new CqAttributesFactory();
			cqAttributesFactory.addCqListener(new MessageProducingCqListener());
			cqAttributes = cqAttributesFactory.create();


			CqQuery existingCqQuery = queryService.getCq( this.queryName );
			if(existingCqQuery !=null){
				queryService.stopCqs(this.queryName);
			}

			cqQuery = queryService.newCq(queryName, queryString, cqAttributes, durable);


		} catch (Throwable e) {
			throw new RuntimeException("Couldn't properly setup the " + getClass().getName(), e);
		}
	}

	/**
	 * Listener that listens for any events being broadcast as a result of
	 * the evaluation of a continuous query {@link CqQuery}. 
	 *
	 */
	class MessageProducingCqListener implements CqListener {

		public void onEvent(CqEvent cqEvent) {
			Message<CqEvent> cqEventMessage = MessageBuilder.withPayload(cqEvent).build();
			sendMessage(cqEventMessage);
		}

		public void onError(CqEvent cqEvent) {
			logger.debug( "error on " + getClass() +  " (a CqListener) "  );
			throw new RuntimeException("error when interacting with region.", cqEvent.getThrowable());
		}

		public void close() {
			logger.debug( getClass() +" close() called");
		}
	}
}
