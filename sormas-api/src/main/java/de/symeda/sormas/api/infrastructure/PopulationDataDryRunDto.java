package de.symeda.sormas.api.infrastructure;

import java.util.Date;

import de.symeda.sormas.api.AgeGroup;
import de.symeda.sormas.api.EntityDto;
import de.symeda.sormas.api.ImportIgnore;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.utils.DataHelper;

public class PopulationDataDryRunDto extends EntityDto {

	private static final long serialVersionUID = -4254008000534611519L;

	public static final String I18N_PREFIX = "PopulationData";

	public static final String REGION = "region";
	public static final String DISTRICT = "district";
	public static final String COMMUNITY = "community";
	public static final String COMMUNITY_EXTID = "CCode";
	public static final String SEX = "sex";
	public static final String AGE_GROUP = "ageGroup";
	public static final String POPULATION = "population";
	public static final String COLLECTION_DATE = "collectionDate";
	public static final String CAMPAIGN = "campaign";
	public static final String SELECTED = "selected";
	public static final String MODALITY = "modality";
	public static final String DISTRICT_STATUS = "districtstatus";



	private RegionReferenceDto region;
	private DistrictReferenceDto district;
	private CommunityReferenceDto community;
	private Sex sex;
	private AgeGroup ageGroup;
	private Integer population;
	private Date collectionDate;
	private CampaignReferenceDto campaign;
	private String selected;
	private String modality;
	private String districtStatus;



	public static PopulationDataDryRunDto build(Date collectionDate) {

		PopulationDataDryRunDto dto = new PopulationDataDryRunDto();
		dto.setUuid(DataHelper.createUuid());
		dto.setCollectionDate(collectionDate);
		return dto;
	}

	public RegionReferenceDto getRegion() {
		return region;
	}

	public void setRegion(RegionReferenceDto region) {
		this.region = region;
	}

	public DistrictReferenceDto getDistrict() {
		return district;
	}

	public void setDistrict(DistrictReferenceDto district) {
		this.district = district;
	}

	public CommunityReferenceDto getCommunity() {
		return community;
	}

	public void setCommunity(CommunityReferenceDto community) {
		this.community = community;
	}

	@ImportIgnore
	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	public AgeGroup getAgeGroup() {
		return ageGroup;
	}

	public void setAgeGroup(AgeGroup ageGroup) {
		this.ageGroup = ageGroup;
	}

	public Integer getPopulation() {
		return population;
	}

	public void setPopulation(Integer population) {
		this.population = population;
	}

	public Date getCollectionDate() {
		return collectionDate;
	}

	public void setCollectionDate(Date collectionDate) {
		this.collectionDate = collectionDate;
	}

	public CampaignReferenceDto getCampaign() {
		return campaign;
	}

	public void setCampaign(CampaignReferenceDto campaign) {
		this.campaign = campaign;
	}

	public String getSelected() {
		return selected;
	}

	public void setSelected(String selected) {
		this.selected = selected;
	}

	public String getModality() {
		return modality;
	}

	public void setModality(String modality) {
		this.modality = modality;
	}
	
	public String getDistrictStatus() {
		return districtStatus;
	}

	public void setDistrictStatus(String districtStatus) {
		this.districtStatus = districtStatus;
	}
	
	
	
}
