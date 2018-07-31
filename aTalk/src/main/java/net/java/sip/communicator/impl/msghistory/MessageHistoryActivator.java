/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.history.HistoryService;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

import org.atalk.service.configuration.ConfigurationService;
import org.atalk.service.resources.ResourceManagementService;
import org.osgi.framework.*;

/**
 * Activates the MessageHistoryService
 *
 * @author Damian Minkov
 */
public class MessageHistoryActivator implements BundleActivator
{
    /**
     * The <tt>BundleContext</tt> of the service.
     */
    static BundleContext bundleContext;
    /**
     * The <tt>Logger</tt> instance used by the <tt>MessageHistoryActivator</tt> class and its
     * instances for logging output.
     */
    private static Logger logger = Logger.getLogger(MessageHistoryActivator.class);
    /**
     * The <tt>MessageHistoryService</tt> reference.
     */
    private static MessageHistoryServiceImpl msgHistoryService = null;
    /**
     * The <tt>ResourceManagementService</tt> reference.
     */
    private static ResourceManagementService resourcesService;
    /**
     * The <tt>MetaContactListService</tt> reference.
     */
    private static MetaContactListService metaCListService;
    /**
     * The <tt>ConfigurationService</tt> reference.
     */
    private static ConfigurationService configService;

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle context.
     *
     * @return the <tt>MetaContactListService</tt> obtained from the bundle context
     */
    public static MetaContactListService getContactListService()
    {
        if (metaCListService == null) {
            metaCListService = ServiceUtils.getService(bundleContext, MetaContactListService.class);
        }
        return metaCListService;
    }

    /**
     * Returns the <tt>MessageHistoryService</tt> registered to the bundle context.
     *
     * @return the <tt>MessageHistoryService</tt> registered to the bundle context
     */
    public static MessageHistoryServiceImpl getMessageHistoryService()
    {
        return msgHistoryService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null) {
            resourcesService = ServiceUtils.getService(bundleContext, ResourceManagementService.class);
        }
        return resourcesService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle context.
     *
     * @return the <tt>ConfigurationService</tt> obtained from the bundle context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null) {
            configService = ServiceUtils.getService(bundleContext, ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Initialize and start message history
     *
     * @param bc the BundleContext
     * @throws Exception if initializing and starting message history service fails
     */
    public void start(BundleContext bc)
            throws Exception
    {
        bundleContext = bc;
        try {
            logger.logEntry();

            ServiceReference refHistory = bundleContext.getServiceReference(HistoryService.class.getName());
            HistoryService historyService = (HistoryService) bundleContext.getService(refHistory);

            // Create and start the message history service.
            msgHistoryService = new MessageHistoryServiceImpl();
            msgHistoryService.setHistoryService(historyService);
            msgHistoryService.start(bundleContext);

            bundleContext.registerService(MessageHistoryService.class.getName(), msgHistoryService, null);

            if (logger.isInfoEnabled())
                logger.info("Message History Service ...[REGISTERED]");
        } finally {
            logger.logExit();
        }

    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {
        if (msgHistoryService != null)
            msgHistoryService.stop(bundleContext);
    }
}
