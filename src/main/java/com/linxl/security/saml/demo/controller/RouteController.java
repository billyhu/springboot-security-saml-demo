package com.linxl.security.saml.demo.controller;

import com.linxl.security.saml.demo.saml.SAMLUser;
import com.linxl.security.saml.demo.saml.SAMLUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class RouteController {

//    @Autowired
//    private MetadataManager metadataManager;

    @RequestMapping("/home")
    public ModelAndView home(@SAMLUser SAMLUserDetails user) {
        ModelAndView homeView = new ModelAndView("home");
        homeView.addObject("userId", user.getUsername());
        homeView.addObject("samlAttributes", user.getAttributes());
        return homeView;
    }

//    @RequestMapping("/idpselection")
//    public ModelAndView idpSelection(HttpServletRequest request) {
//        if (comesFromDiscoveryFilter(request)) {
//            ModelAndView idpSelection = new ModelAndView("idpselection");
//            idpSelection.addObject(SAMLDiscovery.RETURN_URL, request.getAttribute(SAMLDiscovery.RETURN_URL));
//            idpSelection.addObject(SAMLDiscovery.RETURN_PARAM, request.getAttribute(SAMLDiscovery.RETURN_PARAM));
//            Map<String, String> idpNameAliasMap = metadataManager.getIDPEntityNames().stream()
//                    .collect(toMap(identity(), this::getAlias));
//            idpSelection.addObject("idpNameAliasMap", idpNameAliasMap);
//            return idpSelection;
//        }
//        throw new AuthenticationServiceException("SP Discovery flow not detected");
//    }
//
//    private String getAlias(String entityId) {
//        try {
//            return metadataManager.getExtendedMetadata(entityId).getAlias();
//        } catch (MetadataProviderException e) {
//            throw new IllegalStateException("Fail to get alias by entityId " + entityId, e);
//        }
//    }
//
//    private boolean comesFromDiscoveryFilter(HttpServletRequest request) {
//        return request.getAttribute(SAMLConstants.LOCAL_ENTITY_ID) != null &&
//                request.getAttribute(SAMLDiscovery.RETURN_URL) != null &&
//                request.getAttribute(SAMLDiscovery.RETURN_PARAM) != null;
//    }

}
