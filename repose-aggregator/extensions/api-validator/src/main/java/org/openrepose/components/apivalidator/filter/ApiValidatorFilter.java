package org.openrepose.components.apivalidator.filter;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.servlet.InitParameter;
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration1;
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.servlet.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ApiValidatorFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorFilter.class);
    private static final String DEFAULT_CONFIG = "validator.cfg.xml";
    private String config;
    private ApiValidatorHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;
    private MetricsService metricsService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ApiValidatorHandler handler = handlerFactory.newHandler();
        if (handler != null) {
            handler.setFilterChain(chain);
        } else {
            LOG.error("Unable to build API validator handler");
        }
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handler);
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final String configProp = InitParameter.POWER_API_CONFIG_DIR.getParameterName();
        final ServletContext ctx = filterConfig.getServletContext();
        final String configurationRoot = System.getProperty(configProp, ctx.getInitParameter(configProp));
        configurationManager = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext()
                .configurationService();
        metricsService = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext()
                .metricsService();
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new ApiValidatorHandlerFactory(configurationManager, configurationRoot, config, metricsService);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/validator-configuration.xsd");

        try {
            subscribeToConfiguration(filterConfig.getFilterName(), xsdURL, new File(configurationRoot, config).toURI().toString());
        } catch (ParserConfigurationException pce) {
            //todo: log and fail
        } catch (SAXException saxe) {
            //todo: log and fail
        } catch (IOException ioe) {
            //todo: log and fail
        }
    }

    private void subscribeToConfiguration(String filterName, URL xsdURL, String configURL)
            throws IOException, SAXException, ParserConfigurationException {
        final int version = parseVersion(configURL);

        if (version == 1 || version == -1) {
            configurationManager.subscribeTo(filterName, config, xsdURL, handlerFactory, ValidatorConfiguration1.class);
        } else if (version == 2) {
            configurationManager.subscribeTo(filterName, config, xsdURL, handlerFactory, ValidatorConfiguration2.class);
        }
    }

    private int parseVersion(String configURL)
            throws IOException, SAXException, ParserConfigurationException {
        VersioningHandler versioningHandler = new VersioningHandler();

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(versioningHandler);
        xmlReader.parse(configURL);

        return versioningHandler.getVersion();
    }

    private class VersioningHandler extends DefaultHandler {
        private int version;

        public int getVersion() {
            return version;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXParseException {
            if ("validators".equalsIgnoreCase(localName)) {
                final String configuredVersion = attributes.getValue("version");

                if (configuredVersion == null) {
                    version = -1;
                } else {
                    version = Integer.parseInt(configuredVersion);
                }
            }
        }
    }
}
