package uk.gov.dhsc.htbhf.smartstub.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dhsc.htbhf.dwp.model.DWPEligibilityRequest;
import uk.gov.dhsc.htbhf.dwp.model.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.dwp.model.PersonDTO;
import uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.ChildDobGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static uk.gov.dhsc.htbhf.TestConstants.SIMPSON_SURNAME;
import static uk.gov.dhsc.htbhf.TestConstants.SIX_MONTH_OLD;
import static uk.gov.dhsc.htbhf.TestConstants.TWO_CHILDREN_BORN_AT_START_OF_MONTH;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.MATCHED;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_HELD;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_MATCHED;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_SUPPLIED;
import static uk.gov.dhsc.htbhf.dwp.testhelper.DWPEligibilityRequestTestDataFactory.aValidDWPEligibilityRequestWithPerson;
import static uk.gov.dhsc.htbhf.dwp.testhelper.IdAndEligibilityResponseTestDataFactory.*;
import static uk.gov.dhsc.htbhf.dwp.testhelper.PersonDTOTestDataFactory.*;
import static uk.gov.dhsc.htbhf.smartstub.Assertions.assertIsEqualIgnoringHouseholdIdentifier;
import static uk.gov.dhsc.htbhf.smartstub.helper.v2.IdentityAndEligibilityResponseTestDataHelper.addPregnantChildDOBMatch;
import static uk.gov.dhsc.htbhf.smartstub.service.v2.IdentityAndEligibilityService.*;

class IdentityAndEligibilityServiceTest {

    private static final String IDENTITY_MATCH_FAILED_NINO = "XC123456A";
    private static final String IDENTITY_MATCHED_NOT_ELIGIBLE_NINO = "MX123456A";
    private static final String IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_NINO = "MC999999A";
    private static final String IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_PARTIAL_CHILDREN_MATCH_NINO = "MC219999A";
    private static final String IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_FULL_CHILDREN_MATCH_NINO = "MC129999A";
    private static final String IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_NO_CHILDREN_NINO = "MC009999A";
    private static final String NOT_SET = null;

    private IdentityAndEligibilityService service = new IdentityAndEligibilityService();

    @Test
    void shouldFailIdentityMatch() {
        PersonDTO person = aPersonDTOWithNino(IDENTITY_MATCH_FAILED_NINO);
        IdentityAndEligibilityResponse expectedResponse = anIdMatchFailedResponse();
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @Test
    void shouldReturnIdentityMatchedEligibilityNotConfirmed() {
        PersonDTO person = aPersonDTOWithNino(IDENTITY_MATCHED_NOT_ELIGIBLE_NINO);
        IdentityAndEligibilityResponse expectedResponse = anIdMatchedEligibilityNotConfirmedResponse();
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @Test
    void shouldReturnIdentityMatchedEligibilityConfirmedAddressLine1NotMatched() {
        PersonDTO person = aPersonDTOWithSurnameAndNino(ADDRESS_LINE_ONE_NOT_MATCHED_SURNAME, IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_NINO);
        List<LocalDate> childrenDobs = ChildDobGenerator.createDatesOfBirthForChildren(IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_NINO);
        IdentityAndEligibilityResponse expectedResponse = setChildrenDobs(childrenDobs, anIdMatchedEligibilityConfirmedAddressLine1NotMatchedResponse());
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @Test
    void shouldReturnIdentityMatchedEligibilityConfirmedPostcodeNotMatched() {
        PersonDTO person = aPersonDTOWithSurnameAndNino(POSTCODE_NOT_MATCHED_SURNAME, IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_NINO);
        List<LocalDate> childrenDobs = ChildDobGenerator.createDatesOfBirthForChildren(IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_NINO);
        IdentityAndEligibilityResponse expectedResponse = setChildrenDobs(childrenDobs, anIdMatchedEligibilityConfirmedPostcodeNotMatchedResponse());
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @Test
    void shouldReturnIdentityMatchedEligibilityConfirmedForPregnantWomanWithNoChildren() {
        PersonDTO person = aPersonDTOWithSurnameAndNino(SIMPSON_SURNAME, IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_NO_CHILDREN_NINO);
        IdentityAndEligibilityResponse expectedResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(VerificationOutcome.NOT_SET,
                emptyList());
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @Test
    void shouldReturnIdentityMatchedEligibilityConfirmedPregnantDependentNotProvidedChildren() {
        PersonDTO person = aPersonDTOWithPregnantDependantDob(null);
        IdentityAndEligibilityResponse expectedResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(TWO_CHILDREN_BORN_AT_START_OF_MONTH);
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @Test
    void shouldThrowExceptionForExceptionalNino() {
        //Given
        PersonDTO person = aPersonDTOWithSurnameAndNino(SIMPSON_SURNAME, EXCEPTION_NINO);
        DWPEligibilityRequest requestV2 = aValidDWPEligibilityRequestWithPerson(person);
        //When
        IllegalArgumentException thrown = catchThrowableOfType(() -> service.evaluateEligibility(requestV2), IllegalArgumentException.class);
        //Then
        assertThat(thrown).hasMessage("NINO provided (XX999999D) has been configured to trigger an Exception");
    }

    @ParameterizedTest(name = "Surname={0}, mobile outcome={1}, email outcome={2}")
    @MethodSource("verificationOutcomeForSurnameArguments")
    void shouldReturnIdentityMatchedEligibilityConfirmedPartialChildrenMatch(String surname,
                                                                             VerificationOutcome mobileMatchOutcome,
                                                                             VerificationOutcome emailMatchOutcome) {
        PersonDTO person = aPersonDTOWithSurnameAndNino(surname, IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_PARTIAL_CHILDREN_MATCH_NINO);
        IdentityAndEligibilityResponse expectedResponse = addPregnantChildDOBMatch(
                anIdMatchedEligibilityConfirmedUCResponseWithMatches(mobileMatchOutcome, emailMatchOutcome, singletonList(SIX_MONTH_OLD)),
                VerificationOutcome.NOT_SET
        );
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @ParameterizedTest(name = "Surname={0}, mobile outcome={1}, email outcome={2}")
    @MethodSource("verificationOutcomeForSurnameArguments")
    void shouldReturnIdentityMatchedEligibilityConfirmedFullChildrenMatch(String surname,
                                                                          VerificationOutcome mobileMatchOutcome,
                                                                          VerificationOutcome emailMatchOutcome) {
        PersonDTO person = aPersonDTOWithSurnameAndNino(surname, IDENTITY_MATCHED_ELIGIBILITY_CONFIRMED_FULL_CHILDREN_MATCH_NINO);
        IdentityAndEligibilityResponse expectedResponse = addPregnantChildDOBMatch(
                anIdMatchedEligibilityConfirmedUCResponseWithMatches(mobileMatchOutcome, emailMatchOutcome, TWO_CHILDREN_BORN_AT_START_OF_MONTH),
                VerificationOutcome.NOT_SET
        );
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @ParameterizedTest(name = "Surname={0}, email outcome={1}")
    @MethodSource("emailVerificationOutcomeForSurnameArguments")
    void shouldReturnNotProvidedMobileVerificationWhenNoMobileProvided(String surname,
                                                                       VerificationOutcome emailMatchOutcome) {
        PersonDTO person = aPersonDTOWithSurnameAndMobile(surname, NOT_SET);
        IdentityAndEligibilityResponse expectedResponse = addPregnantChildDOBMatch(
                anIdMatchedEligibilityConfirmedUCResponseWithMatches(NOT_SUPPLIED, emailMatchOutcome, TWO_CHILDREN_BORN_AT_START_OF_MONTH),
                VerificationOutcome.NOT_SET
        );
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    @ParameterizedTest(name = "Surname={0}, mobile outcome={1}")
    @MethodSource("mobileVerificationOutcomeForSurnameArguments")
    void shouldReturnNotProvidedEmailVerificationWhenNoEmailAddressProvided(String surname,
                                                                            VerificationOutcome mobileMatchOutcome) {
        PersonDTO person = aPersonDTOWithSurnameAndEmail(surname, NOT_SET);
        IdentityAndEligibilityResponse expectedResponse = addPregnantChildDOBMatch(
                anIdMatchedEligibilityConfirmedUCResponseWithMatches(mobileMatchOutcome, NOT_SUPPLIED, TWO_CHILDREN_BORN_AT_START_OF_MONTH),
                VerificationOutcome.NOT_SET
        );
        runEvaluateEligibilityTest(person, expectedResponse);
    }

    private void runEvaluateEligibilityTest(PersonDTO person, IdentityAndEligibilityResponse expectedResponse) {
        //Given
        DWPEligibilityRequest requestV2 = aValidDWPEligibilityRequestWithPerson(person);
        //When
        IdentityAndEligibilityResponse response = service.evaluateEligibility(requestV2);
        //Then
        assertIsEqualIgnoringHouseholdIdentifier(response, expectedResponse);
    }

    //Arguments are Surname, Mobile Match, Email Match
    private static Stream<Arguments> verificationOutcomeForSurnameArguments() {
        return Stream.of(
                Arguments.of(MOBILE_NOT_HELD_SURNAME, NOT_HELD, MATCHED),
                Arguments.of(EMAIL_NOT_HELD_SURNAME, MATCHED, NOT_HELD),
                Arguments.of(MOBILE_AND_EMAIL_NOT_HELD_SURNAME, NOT_HELD, NOT_HELD),
                Arguments.of(MOBILE_NOT_MATCHED_SURNAME, NOT_MATCHED, MATCHED),
                Arguments.of(EMAIL_NOT_MATCHED_SURNAME, MATCHED, NOT_MATCHED),
                Arguments.of(MOBILE_AND_EMAIL_NOT_MATCHED_SURNAME, NOT_MATCHED, NOT_MATCHED)
        );
    }

    //Arguments are Surname, Email Match
    private static Stream<Arguments> emailVerificationOutcomeForSurnameArguments() {
        return Stream.of(
                Arguments.of(MOBILE_NOT_HELD_SURNAME, MATCHED),
                Arguments.of(EMAIL_NOT_HELD_SURNAME, NOT_HELD),
                Arguments.of(MOBILE_AND_EMAIL_NOT_HELD_SURNAME, NOT_HELD),
                Arguments.of(MOBILE_NOT_MATCHED_SURNAME, MATCHED),
                Arguments.of(EMAIL_NOT_MATCHED_SURNAME, NOT_MATCHED),
                Arguments.of(MOBILE_AND_EMAIL_NOT_MATCHED_SURNAME, NOT_MATCHED)
        );
    }

    //Arguments are Surname, Mobile Match
    private static Stream<Arguments> mobileVerificationOutcomeForSurnameArguments() {
        return Stream.of(
                Arguments.of(MOBILE_NOT_HELD_SURNAME, NOT_HELD),
                Arguments.of(EMAIL_NOT_HELD_SURNAME, MATCHED),
                Arguments.of(MOBILE_AND_EMAIL_NOT_HELD_SURNAME, NOT_HELD),
                Arguments.of(MOBILE_NOT_MATCHED_SURNAME, NOT_MATCHED),
                Arguments.of(EMAIL_NOT_MATCHED_SURNAME, MATCHED),
                Arguments.of(MOBILE_AND_EMAIL_NOT_MATCHED_SURNAME, NOT_MATCHED)
        );
    }

    private IdentityAndEligibilityResponse setChildrenDobs(List<LocalDate> childrenDobs, IdentityAndEligibilityResponse response) {
        return response.toBuilder()
                .dobOfChildrenUnder4(childrenDobs)
                .build();
    }

}
