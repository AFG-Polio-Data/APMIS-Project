package org.sormas.e2etests.entities.services;

import com.github.javafaker.Faker;
import com.google.inject.Inject;
import org.sormas.e2etests.entities.pojo.web.AggregateReport;
import org.sormas.e2etests.enums.DistrictsValues;
import org.sormas.e2etests.enums.RegionsValues;

public class AggregateReportService {
  private final Faker faker;

  @Inject
  public AggregateReportService(Faker faker) {
    this.faker = faker;
  }

  public AggregateReport buildGeneratedAggregateReport() {
    return AggregateReport.builder()
        .year("2003")
        .epiWeek("Wk 1-2003 (12/30 - 1/5)")
        .acuteRespiratoryInfectionsCases(faker.number().numberBetween(2, 12))
        .acuteRespiratoryInfectionsLabConfirmations(faker.number().numberBetween(2, 12))
        .acuteRespiratoryInfectionsDeaths(faker.number().numberBetween(2, 12))
        .acuteViralHepatitisCases(faker.number().numberBetween(2, 12))
        .acuteViralHepatitisLabConfirmations(faker.number().numberBetween(2, 12))
        .acuteViralHepatitisDeaths(faker.number().numberBetween(2, 12))
        .buruliUlcerCases(faker.number().numberBetween(2, 12))
        .buruliUlcerLabConfirmations(faker.number().numberBetween(2, 12))
        .buruliUlcerDeaths(faker.number().numberBetween(2, 12))
        .chikungunyaCases(faker.number().numberBetween(2, 12))
        .chikungunyaLabConfirmations(faker.number().numberBetween(2, 12))
        .chikungunyaDeaths(faker.number().numberBetween(2, 12))
        .diarrheaBloodCases(faker.number().numberBetween(2, 12))
        .diarrheaBloodLabConfirmations(faker.number().numberBetween(2, 12))
        .diarrheaBloodDeaths(faker.number().numberBetween(2, 12))
        .diarrheaDehydrationCases(faker.number().numberBetween(2, 12))
        .diarrheaDehydrationLabConfirmations(faker.number().numberBetween(2, 12))
        .diarrheaDehydrationDeaths(faker.number().numberBetween(2, 12))
        .diphteriaCases(faker.number().numberBetween(2, 12))
        .diphteriaLabConfirmations(faker.number().numberBetween(2, 12))
        .diphteriaDeaths(faker.number().numberBetween(2, 12))
        .functionalHypothalamicAmenorrheaCases(faker.number().numberBetween(2, 12))
        .functionalHypothalamicAmenorrheaLabConfirmations(faker.number().numberBetween(2, 12))
        .functionalHypothalamicAmenorrheaDeaths(faker.number().numberBetween(2, 12))
        .hivCases(faker.number().numberBetween(2, 12))
        .hivLabConfirmations(faker.number().numberBetween(2, 12))
        .hivDeaths(faker.number().numberBetween(2, 12))
        .leprosyCases(faker.number().numberBetween(2, 12))
        .leprosyLabConfirmations(faker.number().numberBetween(2, 12))
        .leprosyDeaths(faker.number().numberBetween(2, 12))
        .lymphaticFilariasisCases(faker.number().numberBetween(2, 12))
        .lymphaticFilariasisLabConfirmations(faker.number().numberBetween(2, 12))
        .lymphaticFilariasisDeaths(faker.number().numberBetween(2, 12))
        .malariaCases(faker.number().numberBetween(2, 12))
        .malariaLabConfirmations(faker.number().numberBetween(2, 12))
        .malariaDeaths(faker.number().numberBetween(2, 12))
        .maternalDeathsCases(faker.number().numberBetween(2, 12))
        .maternalDeathsLabConfirmations(faker.number().numberBetween(2, 12))
        .maternalDeathsDeaths(faker.number().numberBetween(2, 12))
        .neonatalTetanusCases(faker.number().numberBetween(2, 12))
        .neonatalTetanusDeaths(faker.number().numberBetween(2, 12))
        .neonatalTetanusLabConfirmations(faker.number().numberBetween(2, 12))
        .nonNeonatalTetanusCases(faker.number().numberBetween(2, 12))
        .nonNeonatalTetanusLabConfirmations(faker.number().numberBetween(2, 12))
        .nonNeonatalTetanusDeaths(faker.number().numberBetween(2, 12))
        .onchocerciasisCases(faker.number().numberBetween(2, 12))
        .onchocerciasisLabConfirmations(faker.number().numberBetween(2, 12))
        .onchocerciasisDeaths(faker.number().numberBetween(2, 12))
        .perinatalDeathsCases(faker.number().numberBetween(2, 12))
        .perinatalDeathsLabConfirmations(faker.number().numberBetween(2, 12))
        .perinatalDeathsDeaths(faker.number().numberBetween(2, 12))
        .pertussisCases(faker.number().numberBetween(2, 12))
        .pertussisDeaths(faker.number().numberBetween(2, 12))
        .pertussisLabConfirmations(faker.number().numberBetween(2, 12))
        .postImmunizationAdverseEventsMildCases(faker.number().numberBetween(2, 12))
        .postImmunizationAdverseEventsMildLabConfirmations(faker.number().numberBetween(2, 12))
        .postImmunizationAdverseEventsMildDeaths(faker.number().numberBetween(2, 12))
        .postImmunizationAdverseEventsSevereCases(faker.number().numberBetween(2, 12))
        .postImmunizationAdverseEventsSevereLabConfirmations(faker.number().numberBetween(2, 12))
        .postImmunizationAdverseEventsSevereDeaths(faker.number().numberBetween(2, 12))
        .rubellaCases(faker.number().numberBetween(2, 12))
        .rubellaLabConfirmations(faker.number().numberBetween(2, 12))
        .rubellaDeaths(faker.number().numberBetween(2, 12))
        .schistosomiasisCases(faker.number().numberBetween(2, 12))
        .schistosomiasisLabConfirmations(faker.number().numberBetween(2, 12))
        .schistosomiasisDeaths(faker.number().numberBetween(2, 12))
        .snakeBiteCases(faker.number().numberBetween(2, 12))
        .snakeBiteLabConfirmations(faker.number().numberBetween(2, 12))
        .snakeBiteDeaths(faker.number().numberBetween(2, 12))
        .soliTransmittedHelminthsCases(faker.number().numberBetween(2, 12))
        .soliTransmittedHelminthsLabConfirmations(faker.number().numberBetween(2, 12))
        .soliTransmittedHelminthsDeaths(faker.number().numberBetween(2, 12))
        .trachomaCases(faker.number().numberBetween(2, 12))
        .trachomaLabConfirmations(faker.number().numberBetween(2, 12))
        .trachomaDeaths(faker.number().numberBetween(2, 12))
        .trypanosomiasisCases(faker.number().numberBetween(2, 12))
        .trypanosomiasisLabConfirmations(faker.number().numberBetween(2, 12))
        .trypanosomiasisDeaths(faker.number().numberBetween(2, 12))
        .typhoidFeverCases(faker.number().numberBetween(2, 12))
        .typhoidFeverLabConfirmations(faker.number().numberBetween(2, 12))
        .typhoidFeverDeaths(faker.number().numberBetween(2, 12))
        .tuberculosisCases(faker.number().numberBetween(2, 12))
        .tuberculosisConfirmations(faker.number().numberBetween(2, 12))
        .tuberculosisDeaths(faker.number().numberBetween(2, 12))
        .yawsAndEndemicSyphilisCases(faker.number().numberBetween(2, 12))
        .yawsAndEndemicSyphilisLabConfirmations(faker.number().numberBetween(2, 12))
        .yawsAndEndemicSyphilisDeaths(faker.number().numberBetween(2, 12))
        .build();
  }

  public AggregateReport buildEditAggregateReport() {
    return AggregateReport.builder()
        .acuteViralHepatitisCases(faker.number().numberBetween(2, 12))
        .acuteViralHepatitisLabConfirmations(faker.number().numberBetween(2, 12))
        .acuteViralHepatitisDeaths(faker.number().numberBetween(2, 12))
        .buruliUlcerCases(faker.number().numberBetween(2, 12))
        .buruliUlcerLabConfirmations(faker.number().numberBetween(2, 12))
        .buruliUlcerDeaths(faker.number().numberBetween(2, 12))
        .diarrheaBloodCases(faker.number().numberBetween(2, 12))
        .diarrheaBloodLabConfirmations(faker.number().numberBetween(2, 12))
        .diarrheaBloodDeaths(faker.number().numberBetween(2, 12))
        .diarrheaDehydrationCases(faker.number().numberBetween(2, 12))
        .diarrheaDehydrationLabConfirmations(faker.number().numberBetween(2, 12))
        .diarrheaDehydrationDeaths(faker.number().numberBetween(2, 12))
        .diphteriaCases(faker.number().numberBetween(2, 12))
        .diphteriaLabConfirmations(faker.number().numberBetween(2, 12))
        .diphteriaDeaths(faker.number().numberBetween(2, 12))
        .hivCases(faker.number().numberBetween(2, 12))
        .hivLabConfirmations(faker.number().numberBetween(2, 12))
        .hivDeaths(faker.number().numberBetween(2, 12))
        .leprosyCases(faker.number().numberBetween(2, 12))
        .leprosyLabConfirmations(faker.number().numberBetween(2, 12))
        .leprosyDeaths(faker.number().numberBetween(2, 12))
        .lymphaticFilariasisCases(faker.number().numberBetween(2, 12))
        .lymphaticFilariasisLabConfirmations(faker.number().numberBetween(2, 12))
        .lymphaticFilariasisDeaths(faker.number().numberBetween(2, 12))
        .malariaCases(faker.number().numberBetween(2, 12))
        .malariaLabConfirmations(faker.number().numberBetween(2, 12))
        .malariaDeaths(faker.number().numberBetween(2, 12))
        .maternalDeathsCases(faker.number().numberBetween(2, 12))
        .maternalDeathsLabConfirmations(faker.number().numberBetween(2, 12))
        .maternalDeathsDeaths(faker.number().numberBetween(2, 12))
        .neonatalTetanusCases(faker.number().numberBetween(2, 12))
        .neonatalTetanusDeaths(faker.number().numberBetween(2, 12))
        .neonatalTetanusLabConfirmations(faker.number().numberBetween(2, 12))
        .nonNeonatalTetanusCases(faker.number().numberBetween(2, 12))
        .nonNeonatalTetanusLabConfirmations(faker.number().numberBetween(2, 12))
        .nonNeonatalTetanusDeaths(faker.number().numberBetween(2, 12))
        .onchocerciasisCases(faker.number().numberBetween(2, 12))
        .onchocerciasisLabConfirmations(faker.number().numberBetween(2, 12))
        .onchocerciasisDeaths(faker.number().numberBetween(2, 12))
        .perinatalDeathsCases(faker.number().numberBetween(2, 12))
        .perinatalDeathsLabConfirmations(faker.number().numberBetween(2, 12))
        .perinatalDeathsDeaths(faker.number().numberBetween(2, 12))
        .pertussisCases(faker.number().numberBetween(2, 12))
        .pertussisDeaths(faker.number().numberBetween(2, 12))
        .pertussisLabConfirmations(faker.number().numberBetween(2, 12))
        .rubellaCases(faker.number().numberBetween(2, 12))
        .rubellaLabConfirmations(faker.number().numberBetween(2, 12))
        .rubellaDeaths(faker.number().numberBetween(2, 12))
        .schistosomiasisCases(faker.number().numberBetween(2, 12))
        .schistosomiasisLabConfirmations(faker.number().numberBetween(2, 12))
        .schistosomiasisDeaths(faker.number().numberBetween(2, 12))
        .snakeBiteCases(faker.number().numberBetween(2, 12))
        .snakeBiteLabConfirmations(faker.number().numberBetween(2, 12))
        .snakeBiteDeaths(faker.number().numberBetween(2, 12))
        .soliTransmittedHelminthsCases(faker.number().numberBetween(2, 12))
        .soliTransmittedHelminthsLabConfirmations(faker.number().numberBetween(2, 12))
        .soliTransmittedHelminthsDeaths(faker.number().numberBetween(2, 12))
        .trachomaCases(faker.number().numberBetween(2, 12))
        .trachomaLabConfirmations(faker.number().numberBetween(2, 12))
        .trachomaDeaths(faker.number().numberBetween(2, 12))
        .trypanosomiasisCases(faker.number().numberBetween(2, 12))
        .trypanosomiasisLabConfirmations(faker.number().numberBetween(2, 12))
        .trypanosomiasisDeaths(faker.number().numberBetween(2, 12))
        .typhoidFeverCases(faker.number().numberBetween(2, 12))
        .typhoidFeverLabConfirmations(faker.number().numberBetween(2, 12))
        .typhoidFeverDeaths(faker.number().numberBetween(2, 12))
        .tuberculosisCases(faker.number().numberBetween(2, 12))
        .tuberculosisConfirmations(faker.number().numberBetween(2, 12))
        .tuberculosisDeaths(faker.number().numberBetween(2, 12))
        .yawsAndEndemicSyphilisCases(faker.number().numberBetween(2, 12))
        .yawsAndEndemicSyphilisLabConfirmations(faker.number().numberBetween(2, 12))
        .yawsAndEndemicSyphilisDeaths(faker.number().numberBetween(2, 12))
        .build();
  }

  public AggregateReport buildAggredateReportsForDuplicates() {
    return AggregateReport.builder()
        .year("2004")
        .epiWeek("Wk 1-2004 (12/29 - 1/4)")
        .acuteViralHepatitisCases(faker.number().numberBetween(2, 12))
        .buruliUlcerCases(faker.number().numberBetween(2, 12))
        .build();
  }

  public AggregateReport buildAggredateReportsForOneDisease() {
    return AggregateReport.builder()
        .acuteViralHepatitisCases(faker.number().numberBetween(2, 12))
        .build();
  }

  public AggregateReport buildAggregateReportsForDuplicatesWithJurisdiction() {
    return AggregateReport.builder()
        .year("2005")
        .epiWeek("Wk 6-2005 (1/31 - 2/6)")
        .region(RegionsValues.VoreingestellteBundeslander.getName())
        .district(DistrictsValues.VoreingestellterLandkreis.getName())
        .malariaCases(faker.number().numberBetween(2, 12))
        .tuberculosisDeaths(faker.number().numberBetween(2, 12))
        .build();
  }
}