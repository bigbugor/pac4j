package org.pac4j.cas.config;

import org.jasig.cas.client.validation.*;
import org.pac4j.cas.client.CasProxyReceptor;
import org.pac4j.cas.logout.CasLogoutHandler;
import org.pac4j.cas.logout.DefaultCasLogoutHandler;
import org.pac4j.cas.store.ProxyGrantingTicketStore;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.UrlResolver;
import org.pac4j.core.http.DefaultUrlResolver;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.InitializableWebObject;

/**
 * CAS configuration.
 *
 * @author Jerome Leleu
 * @since 1.9.2
 */
public class CasConfiguration extends InitializableWebObject {

    public static final String TICKET_PARAMETER = "ticket";

    public static final String SERVICE_PARAMETER = "service";

    public final static String LOGOUT_REQUEST_PARAMETER = "logoutRequest";

    public final static String SESSION_INDEX_TAG = "SessionIndex";

    public final static String RELAY_STATE_PARAMETER = "RelayState";

    private String encoding = HttpConstants.UTF8_ENCODING;

    private String loginUrl;

    private String prefixUrl;

    private String restUrl;

    private long timeTolerance = 1000L;

    private CasProtocol protocol = CasProtocol.CAS30;

    private boolean renew = false;

    private boolean gateway = false;

    private boolean acceptAnyProxy = false;

    private ProxyList allowedProxyChains = new ProxyList();

    private CasLogoutHandler logoutHandler;

    private TicketValidator defaultTicketValidator;

    private CasProxyReceptor proxyReceptor;

    private UrlResolver urlResolver = new DefaultUrlResolver();

    private String postLogoutUrlParameter = SERVICE_PARAMETER;

    public CasConfiguration() { }

    public CasConfiguration(final String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public CasConfiguration(final String loginUrl, final CasProtocol protocol) {
        this.loginUrl = loginUrl;
        this.protocol = protocol;
    }

    public CasConfiguration(final String loginUrl, final String prefixUrl) {
        this.loginUrl = loginUrl;
        this.prefixUrl = prefixUrl;
    }

    @Override
    protected void internalInit(final WebContext context) {
        if (CommonHelper.isBlank(this.loginUrl) && CommonHelper.isBlank(this.prefixUrl) && CommonHelper.isBlank(this.restUrl)) {
            throw new TechnicalException("loginUrl, prefixUrl and restUrl cannot be all blank");
        }
        CommonHelper.assertNotNull("urlResolver", this.urlResolver);

        initializeClientConfiguration(context);

        initializeLogoutHandler();
    }

    protected void initializeClientConfiguration(final WebContext context) {
        if (this.prefixUrl != null && !this.prefixUrl.endsWith("/")) {
            this.prefixUrl += "/";
        }
        if (CommonHelper.isBlank(this.prefixUrl)) {
            this.prefixUrl = this.loginUrl.replaceFirst("/login$", "/");
        } else if (CommonHelper.isBlank(this.loginUrl)) {
            this.loginUrl = this.prefixUrl + "login";
        }
        if (CommonHelper.isBlank(restUrl)) {
            restUrl = prefixUrl;
            if (!restUrl.endsWith("/")) {
                restUrl += "/";
            }
            restUrl += "v1/tickets";
        }
    }

    protected void initializeLogoutHandler() {
        if (this.logoutHandler == null) {
            this.logoutHandler = new DefaultCasLogoutHandler();
        }
    }

    public TicketValidator retrieveTicketValidator(final WebContext context) {
        if (this.defaultTicketValidator != null) {
            return this.defaultTicketValidator;
        } else {
            if (this.protocol == CasProtocol.CAS10) {
                return buildCas10TicketValidator(context);
            } else if (this.protocol == CasProtocol.CAS20) {
                return buildCas20TicketValidator(context);
            } else if (this.protocol == CasProtocol.CAS20_PROXY) {
                return buildCas20ProxyTicketValidator(context);
            } else if (this.protocol == CasProtocol.CAS30) {
                return buildCas30TicketValidator(context);
            } else if (this.protocol == CasProtocol.CAS30_PROXY) {
                return buildCas30ProxyTicketValidator(context);
            } else if (this.protocol == CasProtocol.SAML) {
                return buildSAMLTicketValidator(context);
            } else {
                throw new TechnicalException("Unable to initialize the TicketValidator for protocol: " + this.protocol);
            }
        }
    }

    protected TicketValidator buildSAMLTicketValidator(final WebContext context) {
        final Saml11TicketValidator saml11TicketValidator = new Saml11TicketValidator(computeFinalPrefixUrl(context));
        saml11TicketValidator.setTolerance(getTimeTolerance());
        saml11TicketValidator.setEncoding(this.encoding);
        return saml11TicketValidator;
    }

    protected TicketValidator buildCas30ProxyTicketValidator(final WebContext context) {
        final Cas30ProxyTicketValidator cas30ProxyTicketValidator = new Cas30ProxyTicketValidator(computeFinalPrefixUrl(context));
        cas30ProxyTicketValidator.setEncoding(this.encoding);
        cas30ProxyTicketValidator.setAcceptAnyProxy(this.acceptAnyProxy);
        cas30ProxyTicketValidator.setAllowedProxyChains(this.allowedProxyChains);
        if (this.proxyReceptor != null) {
            cas30ProxyTicketValidator.setProxyCallbackUrl(this.proxyReceptor.computeFinalCallbackUrl(context));
            cas30ProxyTicketValidator.setProxyGrantingTicketStorage(new ProxyGrantingTicketStore(this.proxyReceptor.getStore()));
        }
        return cas30ProxyTicketValidator;
    }

    protected TicketValidator buildCas30TicketValidator(final WebContext context) {
        final Cas30ServiceTicketValidator cas30ServiceTicketValidator = new Cas30ServiceTicketValidator(computeFinalPrefixUrl(context));
        cas30ServiceTicketValidator.setEncoding(this.encoding);
        if (this.proxyReceptor != null) {
            cas30ServiceTicketValidator.setProxyCallbackUrl(this.proxyReceptor.computeFinalCallbackUrl(context));
            cas30ServiceTicketValidator.setProxyGrantingTicketStorage(new ProxyGrantingTicketStore(this.proxyReceptor.getStore()));
        }
        return cas30ServiceTicketValidator;
    }

    protected TicketValidator buildCas20ProxyTicketValidator(final WebContext context) {
        final Cas20ProxyTicketValidator cas20ProxyTicketValidator = new Cas20ProxyTicketValidator(computeFinalPrefixUrl(context));
        cas20ProxyTicketValidator.setEncoding(this.encoding);
        cas20ProxyTicketValidator.setAcceptAnyProxy(this.acceptAnyProxy);
        cas20ProxyTicketValidator.setAllowedProxyChains(this.allowedProxyChains);
        if (this.proxyReceptor != null) {
            cas20ProxyTicketValidator.setProxyCallbackUrl(this.proxyReceptor.computeFinalCallbackUrl(context));
            cas20ProxyTicketValidator.setProxyGrantingTicketStorage(new ProxyGrantingTicketStore(this.proxyReceptor.getStore()));
        }
        return cas20ProxyTicketValidator;
    }

    protected TicketValidator buildCas20TicketValidator(final WebContext context) {
        final Cas20ServiceTicketValidator cas20ServiceTicketValidator = new Cas20ServiceTicketValidator(computeFinalPrefixUrl(context));
        cas20ServiceTicketValidator.setEncoding(this.encoding);
        if (this.proxyReceptor != null) {
            cas20ServiceTicketValidator.setProxyCallbackUrl(this.proxyReceptor.computeFinalCallbackUrl(context));
            cas20ServiceTicketValidator.setProxyGrantingTicketStorage(new ProxyGrantingTicketStore(this.proxyReceptor.getStore()));
        }
        return cas20ServiceTicketValidator;
    }

    protected TicketValidator buildCas10TicketValidator(final WebContext context) {
        final Cas10TicketValidator cas10TicketValidator = new Cas10TicketValidator(computeFinalPrefixUrl(context));
        cas10TicketValidator.setEncoding(this.encoding);
        return cas10TicketValidator;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public String computeFinalLoginUrl(final WebContext context) {
        return urlResolver.compute(this.loginUrl, context);
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(final String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getPrefixUrl() {
        return prefixUrl;
    }

    public String computeFinalPrefixUrl(final WebContext context) {
        return urlResolver.compute(this.prefixUrl, context);
    }

    public void setPrefixUrl(final String prefixUrl) {
        this.prefixUrl = prefixUrl;
    }

    public long getTimeTolerance() {
        return timeTolerance;
    }

    public void setTimeTolerance(final long timeTolerance) {
        this.timeTolerance = timeTolerance;
    }

    public CasProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(final CasProtocol protocol) {
        this.protocol = protocol;
    }

    public boolean isRenew() {
        return renew;
    }

    public void setRenew(final boolean renew) {
        this.renew = renew;
    }

    public boolean isGateway() {
        return gateway;
    }

    public void setGateway(final boolean gateway) {
        this.gateway = gateway;
    }

    public boolean isAcceptAnyProxy() {
        return acceptAnyProxy;
    }

    public void setAcceptAnyProxy(final boolean acceptAnyProxy) {
        this.acceptAnyProxy = acceptAnyProxy;
    }

    public ProxyList getAllowedProxyChains() {
        return allowedProxyChains;
    }

    public void setAllowedProxyChains(final ProxyList allowedProxyChains) {
        this.allowedProxyChains = allowedProxyChains;
    }

    public CasLogoutHandler getLogoutHandler() {
        return logoutHandler;
    }

    public void setLogoutHandler(final CasLogoutHandler logoutHandler) {
        this.logoutHandler = logoutHandler;
    }

    public TicketValidator getDefaultTicketValidator() {
        return defaultTicketValidator;
    }

    public void setDefaultTicketValidator(final TicketValidator defaultTicketValidator) {
        this.defaultTicketValidator = defaultTicketValidator;
    }

    public CasProxyReceptor getProxyReceptor() {
        return proxyReceptor;
    }

    public void setProxyReceptor(final CasProxyReceptor proxyReceptor) {
        this.proxyReceptor = proxyReceptor;
    }

    public String computeFinalUrl(final String url, final WebContext context) {
        return urlResolver.compute(url, context);
    }

    public String getPostLogoutUrlParameter() {
        return postLogoutUrlParameter;
    }

    public void setPostLogoutUrlParameter(final String postLogoutUrlParameter) {
        this.postLogoutUrlParameter = postLogoutUrlParameter;
    }

    public String getRestUrl() {
        return restUrl;
    }

    public void setRestUrl(final String restUrl) {
        this.restUrl = restUrl;
    }

    public String computeFinalRestUrl(final WebContext context) {
        return urlResolver.compute(this.restUrl, context);
    }

    public UrlResolver getUrlResolver() {
        return urlResolver;
    }

    public void setUrlResolver(final UrlResolver urlResolver) {
        this.urlResolver = urlResolver;
    }

    @Override
    public String toString() {
        return CommonHelper.toString(this.getClass(), "loginUrl", this.loginUrl, "prefixUrl", this.prefixUrl, "restUrl", this.restUrl,
                "protocol", this.protocol, "renew", this.renew, "gateway", this.gateway, "encoding", this.encoding,
                "logoutHandler", this.logoutHandler, "acceptAnyProxy", this.acceptAnyProxy, "allowedProxyChains", this.allowedProxyChains,
                "proxyReceptor", this.proxyReceptor, "timeTolerance", this.timeTolerance, 
                "postLogoutUrlParameter", this.postLogoutUrlParameter,
                "defaultTicketValidator", this.defaultTicketValidator, "urlResolver", this.urlResolver);
    }
}
