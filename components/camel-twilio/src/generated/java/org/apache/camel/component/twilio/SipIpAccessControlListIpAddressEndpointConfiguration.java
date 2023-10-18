/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 */
package org.apache.camel.component.twilio;

import org.apache.camel.spi.ApiMethod;
import org.apache.camel.spi.ApiParam;
import org.apache.camel.spi.ApiParams;
import org.apache.camel.spi.Configurer;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel endpoint configuration for {@link com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddress}.
 */
@ApiParams(apiName = "sip-ip-access-control-list-ip-address", 
           description = "",
           apiMethods = {@ApiMethod(methodName = "creator", signatures={"com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressCreator creator(String pathIpAccessControlListSid, String friendlyName, String ipAddress)", "com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressCreator creator(String pathAccountSid, String pathIpAccessControlListSid, String friendlyName, String ipAddress)"}), @ApiMethod(methodName = "deleter", signatures={"com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressDeleter deleter(String pathIpAccessControlListSid, String pathSid)", "com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressDeleter deleter(String pathAccountSid, String pathIpAccessControlListSid, String pathSid)"}), @ApiMethod(methodName = "fetcher", signatures={"com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressFetcher fetcher(String pathIpAccessControlListSid, String pathSid)", "com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressFetcher fetcher(String pathAccountSid, String pathIpAccessControlListSid, String pathSid)"}), @ApiMethod(methodName = "reader", signatures={"com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressReader reader(String pathIpAccessControlListSid)", "com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressReader reader(String pathAccountSid, String pathIpAccessControlListSid)"}), @ApiMethod(methodName = "updater", signatures={"com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressUpdater updater(String pathIpAccessControlListSid, String pathSid)", "com.twilio.rest.api.v2010.account.sip.ipaccesscontrollist.IpAddressUpdater updater(String pathAccountSid, String pathIpAccessControlListSid, String pathSid)"}), }, aliases = {"^creator$=create", "^deleter$=delete", "^fetcher$=fetch", "^reader$=read", "^updater$=update"})
@UriParams
@Configurer(extended = true)
public final class SipIpAccessControlListIpAddressEndpointConfiguration extends TwilioConfiguration {
    @UriParam
    @ApiParam(optional = false, apiMethods = {@ApiMethod(methodName = "creator")})
    private String friendlyName;
    @UriParam
    @ApiParam(optional = false, apiMethods = {@ApiMethod(methodName = "creator")})
    private String ipAddress;
    @UriParam
    @ApiParam(optional = false, apiMethods = {@ApiMethod(methodName = "creator"), @ApiMethod(methodName = "deleter"), @ApiMethod(methodName = "fetcher"), @ApiMethod(methodName = "reader"), @ApiMethod(methodName = "updater")})
    private String pathAccountSid;
    @UriParam
    @ApiParam(optional = false, apiMethods = {@ApiMethod(methodName = "creator"), @ApiMethod(methodName = "creator"), @ApiMethod(methodName = "deleter"), @ApiMethod(methodName = "deleter"), @ApiMethod(methodName = "fetcher"), @ApiMethod(methodName = "fetcher"), @ApiMethod(methodName = "reader"), @ApiMethod(methodName = "reader"), @ApiMethod(methodName = "updater"), @ApiMethod(methodName = "updater")})
    private String pathIpAccessControlListSid;
    @UriParam
    @ApiParam(optional = false, apiMethods = {@ApiMethod(methodName = "deleter"), @ApiMethod(methodName = "fetcher"), @ApiMethod(methodName = "updater")})
    private String pathSid;

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPathAccountSid() {
        return pathAccountSid;
    }

    public void setPathAccountSid(String pathAccountSid) {
        this.pathAccountSid = pathAccountSid;
    }

    public String getPathIpAccessControlListSid() {
        return pathIpAccessControlListSid;
    }

    public void setPathIpAccessControlListSid(String pathIpAccessControlListSid) {
        this.pathIpAccessControlListSid = pathIpAccessControlListSid;
    }

    public String getPathSid() {
        return pathSid;
    }

    public void setPathSid(String pathSid) {
        this.pathSid = pathSid;
    }
}
