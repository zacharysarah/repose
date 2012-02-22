package com.rackspace.papi.components.identity.content.auth;

import com.rackspace.papi.components.identity.content.auth.config.ContentIdentityAuthConfig;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import java.io.IOException;
import javax.servlet.*;
import org.slf4j.Logger;

public class ContentIdentityAuthFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentIdentityAuthFilter.class);
    private ContentIdentityAuthHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("content-identity-auth.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new ContentIdentityAuthHandlerFactory();

        configurationManager.subscribeTo("content-identity-auth.cfg.xml", handlerFactory, ContentIdentityAuthConfig.class);
    }
}
