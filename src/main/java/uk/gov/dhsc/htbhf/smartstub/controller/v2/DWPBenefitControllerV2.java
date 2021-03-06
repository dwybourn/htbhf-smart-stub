package uk.gov.dhsc.htbhf.smartstub.controller.v2;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dhsc.htbhf.dwp.model.DWPEligibilityRequest;
import uk.gov.dhsc.htbhf.dwp.model.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.smartstub.service.v2.IdentityAndEligibilityService;

@RestController
@RequestMapping("/v2/dwp/benefits")
@Slf4j
@AllArgsConstructor
public class DWPBenefitControllerV2 {

    private IdentityAndEligibilityService identityAndEligibilityService;

    /**
     * Determines the eligibility of the claimant from the given request details. The request
     * object is built from header values in the request using {@link DwpEligibilityRequestResolver}.
     * The request object is validated, but due to having to use the argument resolver, this
     * is done manually in {@link DwpEligibilityRequestResolver} rather than using Spring's @Valid
     * annotation because this doesn't work on request parameters which are built using
     * an argument resolver.
     *
     * @param request The valid request object built up by {@link DwpEligibilityRequestResolver}
     * @return The identity and eligibility response.
     */
    @GetMapping
    public IdentityAndEligibilityResponse determineEligibility(DWPEligibilityRequest request) {
        log.debug("Received DWP eligibility request: {}", request);
        IdentityAndEligibilityResponse identityAndEligibilityResponse = identityAndEligibilityService.evaluateEligibility(request);
        log.debug("Returning identity and eligibility response: {}", identityAndEligibilityResponse);
        return identityAndEligibilityResponse;
    }

}
