package de.symeda.sormas.backend.infrastructure;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;

import com.vladmihalcea.hibernate.type.util.SQLExtractor;

import de.symeda.sormas.api.AgeGroup;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramCriteria;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.infrastructure.InfrastructureHelper;
import de.symeda.sormas.api.infrastructure.PopulationDataCriteria;
import de.symeda.sormas.api.infrastructure.PopulationDataDryRunCriteria;
import de.symeda.sormas.api.infrastructure.PopulationDataDryRunDto;
import de.symeda.sormas.api.infrastructure.PopulationDataDryRunFacade;
import de.symeda.sormas.api.infrastructure.PopulationDataDto;
import de.symeda.sormas.api.infrastructure.PopulationDataFacade;
import de.symeda.sormas.api.infrastructure.PopulationDataFauxDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.statistics.StatisticsCaseCriteria;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.backend.campaign.Campaign;
import de.symeda.sormas.backend.campaign.CampaignFacadeEjb;
import de.symeda.sormas.backend.campaign.CampaignService;
import de.symeda.sormas.backend.common.AbstractDomainObject;
import de.symeda.sormas.backend.common.CriteriaBuilderHelper;
import de.symeda.sormas.backend.infrastructure.area.Area;
import de.symeda.sormas.backend.infrastructure.community.Community;
import de.symeda.sormas.backend.infrastructure.community.CommunityFacadeEjb;
import de.symeda.sormas.backend.infrastructure.community.CommunityService;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.district.DistrictFacadeEjb;
import de.symeda.sormas.backend.infrastructure.district.DistrictService;
import de.symeda.sormas.backend.infrastructure.region.Region;
import de.symeda.sormas.backend.infrastructure.region.RegionFacadeEjb;
import de.symeda.sormas.backend.infrastructure.region.RegionService;
import de.symeda.sormas.backend.util.DtoHelper;
import de.symeda.sormas.backend.util.ModelConstants;
import de.symeda.sormas.backend.util.QueryHelper;
import org.apache.poi.util.SystemOutLogger;

@Stateless(name = "PopulationDataDryRunFacade")
public class PopulationDataDryRunFacadeEjb implements PopulationDataDryRunFacade {

	@PersistenceContext(unitName = ModelConstants.PERSISTENCE_UNIT_NAME)
	private EntityManager em;

	@EJB
	private PopulationDataService service;

	@EJB
	private PopulationDataDryRunService populationDataDryRunService;
	@EJB
	private RegionService regionService;
	@EJB
	private DistrictService districtService;
	@EJB
	private CommunityService communityService;
	@EJB
	private CampaignService campaignService;

	@Override
	public Integer getRegionPopulation(String regionUuid, PopulationDataCriteria critariax) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);

		PopulationDataCriteria criteria = new PopulationDataCriteria().ageGroupIsNull(true).sexIsNull(true)
				.districtIsNull(true).communityIsNull(true).region(new RegionReferenceDto(regionUuid, null, null));
		Predicate filter = service.buildCriteriaFilter(criteria, cb, root);

		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), criteria.getCampaign().getUuid()));

		cq.where(filter, campaignFilter);

		cq.where(filter);
		cq.select(root.get(PopulationData.POPULATION));

		return QueryHelper.getSingleResult(em, cq);
	}

	@Override
	public Integer getProjectedRegionPopulation(String regionUuid, PopulationDataCriteria critariax) {

		Float growthRate = regionService.getByUuid(regionUuid).getGrowthRate();

		if (growthRate == null || growthRate == 0) {
			return getRegionPopulation(regionUuid, critariax);
		}

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationData> cq = cb.createQuery(PopulationData.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);

		PopulationDataCriteria criteria = new PopulationDataCriteria().ageGroupIsNull(true).sexIsNull(true)
				.districtIsNull(true).communityIsNull(true).region(new RegionReferenceDto(regionUuid, null, null));
		Predicate filter = service.buildCriteriaFilter(criteria, cb, root);
		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), criteria.getCampaign().getUuid()));

		cq.where(filter, campaignFilter);

		try {
			PopulationData populationData = em.createQuery(cq).getSingleResult();
			return InfrastructureHelper.getProjectedPopulation(populationData.getPopulation(),
					populationData.getCollectionDate(), growthRate);
		} catch (NoResultException | NonUniqueResultException e) {
			return null;
		}
	}

	@Override
	public Integer getDistrictPopulation(String districtUuid, PopulationDataCriteria critariax) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);

		PopulationDataCriteria criteria = new PopulationDataCriteria().ageGroupIsNull(true).sexIsNull(true)
				.communityIsNull(true).district(new DistrictReferenceDto(districtUuid, null, null));
		Predicate filter = service.buildCriteriaFilter(criteria, cb, root);
		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), criteria.getCampaign().getUuid()));

		cq.where(filter, campaignFilter);
		cq.select(root.get(PopulationData.POPULATION));

		return QueryHelper.getSingleResult(em, cq);
	}

	@Override
	public Integer getDistrictPopulationByType(String districtUuid, String campaignUuid, AgeGroup ageGroup) {

		System.out.println("------------!!!!!!!!!!!!!!!!!!!!!!: ");

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);
		Join<PopulationData, District> districtJoin = root.join(PopulationData.DISTRICT);

		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), campaignUuid));
		Predicate districtFilter = cb.and(cb.equal(districtJoin.get(District.UUID), districtUuid));
		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationData.AGE_GROUP), ageGroup));

		cq.where(campaignFilter, districtFilter, ageFilter);
		cq.select(root.get(PopulationData.POPULATION));

		System.out.println("------------!!!!!!!!!!!!!!!!!!!!!!: " + SQLExtractor.from(em.createQuery(cq)));

		return QueryHelper.getSingleResult(em, cq);
	}

	@Override
	public Integer getProjectedDistrictPopulation(String districtUuid, PopulationDataCriteria critariax) {

		Float growthRate = districtService.getByUuid(districtUuid).getGrowthRate();

		if (growthRate == null || growthRate == 0) {
			return getDistrictPopulation(districtUuid, critariax);
		}

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationData> cq = cb.createQuery(PopulationData.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);
		PopulationDataCriteria criteria = new PopulationDataCriteria().ageGroupIsNull(true).sexIsNull(true)
				.district(new DistrictReferenceDto(districtUuid, null, null));
		Predicate filter = service.buildCriteriaFilter(criteria, cb, root);

		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), criteria.getCampaign().getUuid()));

		cq.where(filter, campaignFilter);

		try {
			PopulationData populationData = em.createQuery(cq).getSingleResult();
			return InfrastructureHelper.getProjectedPopulation(populationData.getPopulation(),
					populationData.getCollectionDate(), growthRate);
		} catch (NoResultException | NonUniqueResultException e) {
			return null;
		}
	}

	@Override
	public void savePopulationDatax(@Valid List<PopulationDataDryRunDto> populationDataList,
			@Valid List<PopulationDataFauxDto> PopulationDataFauxDto, boolean isFauxData)
			throws ValidationRuntimeException {

//		if(isFauxData){
//			for (PopulationDataFauxDto populationData : PopulationDataFauxDto) {
//				validate(populationData);
////				PopulationDataFaux entity = fromDtox(populationData, true);
////				populationDataDryRunService.ensurePersisted(entity);
//			}
//		}else {
//			for (PopulationDataDto populationData : populationDataList) {
//				validate(populationData);
//				PopulationData entity = fromDto(populationData, true);
//				service.ensurePersisted(entity);
//			}
//		}

	}

	@Override
	public void savePopulationData(@Valid List<PopulationDataDryRunDto> populationDataList)
			throws ValidationRuntimeException {

		for (PopulationDataDryRunDto populationData : populationDataList) {
			System.out.println(populationData.getAgeGroup() + "11111111133333" + populationData.getModality() + "11111111133333"+ populationData.getPopulation() +  populationData.getDistrictStatus());
			
			
			validate(populationData);
			PopulationDataDryRun entity = fromDto(populationData, false);
			populationDataDryRunService.ensurePersisted(entity);
		}
	}

	@Override
	public List<PopulationDataDryRunDto> getDistrictPopulationByTypeUsingUUIDs(String districtUuid, String campaignUuid,
			AgeGroup ageGroup) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationDataDryRun> cq = cb.createQuery(PopulationDataDryRun.class);
		Root<PopulationDataDryRun> root = cq.from(PopulationDataDryRun.class);
		Join<PopulationDataDryRun, Campaign> campaignJoin = root.join(PopulationDataDryRun.CAMPAIGN);
		Join<PopulationDataDryRun, District> districtJoin = root.join(PopulationDataDryRun.DISTRICT);

		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), campaignUuid));
		Predicate districtFilter = cb.and(cb.equal(districtJoin.get(District.UUID), districtUuid));
		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationDataDryRun.AGE_GROUP), ageGroup));

		cq.where(campaignFilter, districtFilter, ageFilter);

		System.out.println("zzzzzzDEBUGGER 5678ijhyuioYYYYYY" + SQLExtractor.from(em.createQuery(cq)) + ageFilter);

		return em.createQuery(cq).getResultStream().map(populationData -> toDto(populationData))
				.collect(Collectors.toList());
	}

	@Override
	public List<PopulationDataDryRunDto> getAllPopulationData() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationDataDryRun> cq = cb.createQuery(PopulationDataDryRun.class);
		Root<PopulationDataDryRun> root = cq.from(PopulationDataDryRun.class);
		// System.out.println("DEBUGGER ----- "+ criteria.getCampaign()!= null);

//		Predicate filter = service.buildCriteriaFilter( cb, root);
//		if (criteria.getCampaign() != null) {
//			Predicate filter_ = CriteriaBuilderHelper.and(cb, filter,
//					cb.equal(root.join(PopulationData.CAMPAIGN, JoinType.LEFT).get(Campaign.UUID),
//							criteria.getCampaign().getUuid()));
//			Predicate filterx = CriteriaBuilderHelper.and(cb, filter_);
//
//			cq.where(filterx);
//		} else {
//			cq.where(filter);
//		}

		System.out.println("zzzzzzDEBUGGER 5678ijhyuio" + SQLExtractor.from(em.createQuery(cq)));

		return em.createQuery(cq).getResultStream().map(populationData -> toDto(populationData))
				.collect(Collectors.toList());
//		return null;
	}

	@Override
	public List<PopulationDataDryRunDto> getPopulationData(PopulationDataDryRunCriteria criteria) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationDataDryRun> cq = cb.createQuery(PopulationDataDryRun.class);
		Root<PopulationDataDryRun> root = cq.from(PopulationDataDryRun.class);
		// System.out.println("DEBUGGER ----- "+ criteria.getCampaign()!= null);

		Predicate filter = populationDataDryRunService.buildCriteriaFilter(criteria, cb, root);
		if (criteria.getCampaign() != null) {
			Predicate filter_ = CriteriaBuilderHelper.and(cb, filter,
					cb.equal(root.join(PopulationData.CAMPAIGN, JoinType.LEFT).get(Campaign.UUID),
							criteria.getCampaign().getUuid()));
			Predicate filterx = CriteriaBuilderHelper.and(cb, filter_, cb.equal(root.get("selected"), true));

			cq.where(filterx);
		} else {
			cq.where(filter);
		}

		System.out.println("DEBUGGER 5678ijhyuio" + SQLExtractor.from(em.createQuery(cq)));

		return em.createQuery(cq).getResultStream().map(populationData -> toDto(populationData))
				.collect(Collectors.toList());
	}

	public List<PopulationDataDryRunDto> getPopulationDataSelected(PopulationDataDryRunCriteria criteria) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationDataDryRun> cq = cb.createQuery(PopulationDataDryRun.class);
		Root<PopulationDataDryRun> root = cq.from(PopulationDataDryRun.class);
		// System.out.println("DEBUGGER ----- "+ criteria.getCampaign()!= null);

		Predicate filter = populationDataDryRunService.buildCriteriaFilter(criteria, cb, root);
		if (criteria.getCampaign() != null) {
			Predicate filter_ = CriteriaBuilderHelper.and(cb, filter,
					cb.equal(root.join(PopulationDataDryRun.CAMPAIGN, JoinType.LEFT).get(Campaign.UUID),
							criteria.getCampaign().getUuid()));
			Predicate filterx = CriteriaBuilderHelper.and(cb, filter_, cb.equal(root.get("selected"), true));
			Predicate selectedFilter = cb.and(cb.equal(root.get(PopulationDataDryRun.SELECTED), true));

			cq.where(filterx, selectedFilter);
		} else {
			cq.where(filter);
		}

		System.out.println("DEBUGGER 5678ijhyuio" + SQLExtractor.from(em.createQuery(cq)));

		return em.createQuery(cq).getResultStream().map(populationData -> toDto(populationData))
				.collect(Collectors.toList());
	}

	@Override
	public List<PopulationDataDryRunDto> getPopulationDataImportChecker(PopulationDataDryRunCriteria criteria) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationDataDryRun> cq = cb.createQuery(PopulationDataDryRun.class);
		Root<PopulationDataDryRun> root = cq.from(PopulationDataDryRun.class);
		// System.out.println("DEBUGGER ----- "+ criteria.getCampaign()!= null);

		Predicate filter = populationDataDryRunService.buildCriteriaFilter(criteria, cb, root);
		if (criteria.getCampaign() != null) {
			Predicate filter_ = CriteriaBuilderHelper.and(cb, filter,
					cb.equal(root.join(PopulationDataDryRun.CAMPAIGN, JoinType.LEFT).get(Campaign.UUID),
							criteria.getCampaign().getUuid()));
			// Predicate filterx = CriteriaBuilderHelper.and(cb, filter_,
			// cb.equal(root.get("selected"), null));

			cq.where(filter_);
		} else {
			cq.where(filter);
		}

		System.out.println("DEBUGGER 5678ijhyuio" + SQLExtractor.from(em.createQuery(cq)));

		return em.createQuery(cq).getResultStream().map(populationData -> toDto(populationData))
				.collect(Collectors.toList());
	}

	@Override
	public List<PopulationDataDryRunDto> getPopulationDataWithCriteria(String campUuid) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationDataDryRun> cq = cb.createQuery(PopulationDataDryRun.class);
		Root<PopulationDataDryRun> root = cq.from(PopulationDataDryRun.class);

		Predicate filter_ = cb
				.and(cb.equal(root.join(PopulationDataDryRun.CAMPAIGN, JoinType.LEFT).get(Campaign.UUID), campUuid));
		Predicate filterx = CriteriaBuilderHelper.and(cb, filter_, cb.equal(root.get(PopulationData.SELECTED), true));
//		Predicate filterxx = CriteriaBuilderHelper.and(cb, filterx,
//				cb.equal(root.get(PopulationData.AGE_GROUP), AgeGroup.AGE_0_4));
//		Predicate filterxxx = CriteriaBuilderHelper.and(cb, filterxx,
//				cb.equal(root.get(PopulationData.AGE_GROUP), AgeGroup.AGE_5_10));

		cq.where(filterx);

//
		System.out.println(campUuid + "DEBUGGER 5678ijhyuio  getPopulationDataWithCriteria  "
				+ SQLExtractor.from(em.createQuery(cq)));

		return em.createQuery(cq).getResultStream().map(populationData -> toDto(populationData))
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object[]> getPopulationDataForExport(String campaignUuid) {
//TODO addd campaign to the selection
		//@formatter:off
		String qry = "SELECT "
				+ Area.TABLE_NAME + "." + Area.NAME + " AS areaname, "
				+ Area.TABLE_NAME + "." + Area.EXTERNAL_ID + " AS rcode, "
				+ Region.TABLE_NAME + "." + Region.NAME + " AS regionname, "
				+ Region.TABLE_NAME + "." + Region.EXTERNAL_ID + " AS pcode, "
				+ District.TABLE_NAME + "." + District.NAME + " AS districtname, "
				+ District.TABLE_NAME + "." + District.EXTERNAL_ID + " AS dcode, "
				+ Community.TABLE_NAME + "." + Community.NAME + " AS communityname," 
				+ Campaign.TABLE_NAME + "." + Campaign.UUID + " AS campaignname, " 
				
				+ PopulationData.AGE_GROUP + ", "
				+ PopulationData.SEX + ", " 
				+ PopulationData.MODALITY  + ", "
				+ PopulationData.DISTRICT_STATUS  + ", "
				+ PopulationData.POPULATION 
				+ " FROM " + PopulationData.TABLE_NAME
				+ " LEFT JOIN " + Campaign.TABLE_NAME + " ON " + PopulationData.CAMPAIGN + "_id = "
				+ Campaign.TABLE_NAME + "." + Campaign.ID
				
				+ " LEFT JOIN " + Region.TABLE_NAME + " ON " + PopulationData.REGION + "_id = "
					+ Region.TABLE_NAME + "." + Region.ID
					
				+ " LEFT JOIN " + Area.TABLE_NAME + " ON " + Region.TABLE_NAME + ".area_id = "
				+ Area.TABLE_NAME + "." + Area.ID
					
				+ " LEFT JOIN " + District.TABLE_NAME + " ON "
					+ PopulationData.DISTRICT + "_id = " + District.TABLE_NAME + "." + District.ID
					
				+ " LEFT JOIN " + Community.TABLE_NAME + " ON "
					+ PopulationData.COMMUNITY + "_id = " + Community.TABLE_NAME + "." + Community.ID
					+" where "+ Campaign.TABLE_NAME + "." + Campaign.UUID +" = '"+campaignUuid+"' " 
				+ " ORDER BY campaignname, regionname, districtname, communityname, pcode, dcode asc NULLS FIRST";
		System.out.println("__________________: "+qry);
		//@formatter:on
		return em.createNativeQuery(qry).getResultList();

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getMissingPopulationDataForStatistics(StatisticsCaseCriteria criteria, boolean groupByRegion,
			boolean groupByDistrict, boolean groupBySex, boolean groupByAgeGroup) {

		StringBuilder regionsIn = new StringBuilder();
		StringBuilder districtsIn = new StringBuilder();
		StringBuilder sexesIn = new StringBuilder();
		StringBuilder ageGroupsIn = new StringBuilder();
		List<Object> parameters = new ArrayList<>();

		if (!CollectionUtils.isEmpty(criteria.getRegions()) && CollectionUtils.isEmpty(criteria.getDistricts())) {
			List<Long> regionIds = regionService.getIdsByReferenceDtos(criteria.getRegions());
			QueryHelper.appendInFilterValues(regionsIn, parameters, regionIds, entry -> entry);
		}
		if (!CollectionUtils.isEmpty(criteria.getDistricts())) {
			List<Long> districtIds = districtService.getIdsByReferenceDtos(criteria.getDistricts());
			QueryHelper.appendInFilterValues(districtsIn, parameters, districtIds, entry -> entry);
		}
		if (!CollectionUtils.isEmpty(criteria.getSexes())) {
			QueryHelper.appendInFilterValues(sexesIn, parameters, criteria.getSexes(), entry -> entry.name());
		}
		if (!CollectionUtils.isEmpty(criteria.getAgeGroups())) {
			QueryHelper.appendInFilterValues(ageGroupsIn, parameters, criteria.getAgeGroups(), entry -> entry.name());
		}

		StringBuilder queryBuilder = new StringBuilder();
		if (!groupByDistrict && CollectionUtils.isEmpty(criteria.getDistricts())) {
			queryBuilder.append("SELECT ").append(Region.ID).append(" FROM ").append(Region.TABLE_NAME);

			if (regionsIn.length() > 0) {
				queryBuilder.append(" WHERE ").append(Region.ID).append(" IN ").append(regionsIn);
			}

			//@formatter:off
			queryBuilder.append(" EXCEPT SELECT ").append(PopulationData.REGION).append("_id FROM ").append(PopulationData.TABLE_NAME)
			.append(" WHERE ").append(PopulationData.TABLE_NAME).append(".").append(PopulationData.DISTRICT).append("_id IS NULL").append(" AND ");
			//@formatter:on
		} else {
			queryBuilder.append("SELECT ").append(District.ID).append(" FROM ").append(District.TABLE_NAME);

			if (districtsIn.length() > 0) {
				queryBuilder.append(" WHERE ").append(District.ID).append(" IN ").append(districtsIn);
			}

			//@formatter:off
			queryBuilder.append(" EXCEPT SELECT ").append(PopulationData.DISTRICT).append("_id FROM ").append(PopulationData.TABLE_NAME).append(" WHERE ");
			//@formatter:on
		}

		queryBuilder.append(PopulationData.TABLE_NAME).append(".").append(PopulationData.SEX);
		if (sexesIn.length() > 0) {
			queryBuilder.append(" IN ").append(sexesIn);
		} else {
			queryBuilder.append(groupBySex ? " IS NOT NULL " : " IS NULL ");
		}

		queryBuilder.append(" AND ").append(PopulationData.TABLE_NAME).append(".").append(PopulationData.AGE_GROUP);
		if (ageGroupsIn.length() > 0) {
			queryBuilder.append(" IN ").append(ageGroupsIn);
		} else {
			queryBuilder.append(groupByAgeGroup ? " IS NOT NULL " : " IS NULL ");
		}

		Query query = em.createNativeQuery(queryBuilder.toString());
		for (int i = 0; i < parameters.size(); i++) {
			query.setParameter(i + 1, parameters.get(i));
		}

		return query.getResultList();
	}

	public Integer getAreaPopulation(String areaUuid, AgeGroup ageGroup, CampaignDiagramCriteria criteria) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Region> regionJoin = root.join(PopulationData.REGION);
		Join<Region, Area> areaJoin = regionJoin.join(Region.AREA);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);

		Predicate areaFilter = cb.equal(areaJoin.get(Area.UUID), areaUuid);
		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationData.AGE_GROUP), ageGroup));
		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), criteria.getCampaign().getUuid()));

		cq.where(areaFilter, ageFilter, campaignFilter);
		cq.select(root.get(PopulationData.POPULATION));

		TypedQuery query = em.createQuery(cq);
		try {
			Integer totalPopulation = 0;
			for (Object i : query.getResultList()) {
				if (Objects.nonNull(i)) {
					totalPopulation = totalPopulation + (Integer) i;
				}
			}
			return totalPopulation;
		} catch (NoResultException e) {
			return null;
		}
	}

	public Integer getAreaPopulationSelected(String areaUuid, AgeGroup ageGroup, CampaignDiagramCriteria criteria) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Region> regionJoin = root.join(PopulationData.REGION);
		Join<Region, Area> areaJoin = regionJoin.join(Region.AREA);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);

		Predicate areaFilter = cb.equal(areaJoin.get(Area.UUID), areaUuid);
		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationData.AGE_GROUP), ageGroup));
		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), criteria.getCampaign().getUuid()));
		Predicate selectedFilter = cb.and(cb.equal(root.get(PopulationData.SELECTED), true));

		cq.where(areaFilter, ageFilter, campaignFilter, selectedFilter);
		cq.select(root.get(PopulationData.POPULATION));

		TypedQuery query = em.createQuery(cq);
		try {
			Integer totalPopulation = 0;
			for (Object i : query.getResultList()) {
				if (Objects.nonNull(i)) {
					totalPopulation = totalPopulation + (Integer) i;
				}
			}
			return totalPopulation;
		} catch (NoResultException e) {
			return null;
		}
	}

	public Integer getAreaPopulationByUuid(String areaUuid, AgeGroup ageGroup, CampaignDiagramCriteria criteria) {
		// System.out.println("DEBUGGER 5678ijhyuio ___xxxxxccccc
		// "+criteria.getCampaign().getUuid());
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Region> regionJoin = root.join(PopulationData.REGION);
		Join<Region, Area> areaJoin = regionJoin.join(Region.AREA);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);

		Predicate areaFilter = cb.equal(areaJoin.get(Area.UUID), areaUuid);
		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationData.AGE_GROUP), ageGroup));
		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), criteria.getCampaign().getUuid()));
		Predicate filter_ = CriteriaBuilderHelper.and(cb, campaignFilter, cb.equal(root.get("selected"), true));

		cq.where(areaFilter, ageFilter, filter_);
		cq.select(root.get(PopulationData.POPULATION));
		// System.out.println("DEBUGGER 5678ijhyuio
		// ___xxxxxcccccc____TOtalpopulation____________________________
		// "+SQLExtractor.from(em.createQuery(cq)));

		TypedQuery query = em.createQuery(cq);
		try {
			Integer totalPopulation = 0;
			for (Object i : query.getResultList()) {
				if (Objects.nonNull(i)) {
					totalPopulation = totalPopulation + (Integer) i;
				}
			}
			return totalPopulation;
		} catch (NoResultException e) {
			return null;
		}
	}

	public Integer getAreaPopulationByUuidSelect(String areaUuid, AgeGroup ageGroup, CampaignDiagramCriteria criteria) {
		// System.out.println("DEBUGGER 5678ijhyuio ___xxxxxccccc
		// "+criteria.getCampaign().getUuid());
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);
		Join<PopulationData, Region> regionJoin = root.join(PopulationData.REGION);
		Join<Region, Area> areaJoin = regionJoin.join(Region.AREA);
		Join<PopulationData, Campaign> campaignJoin = root.join(PopulationData.CAMPAIGN);

		Predicate areaFilter = cb.equal(areaJoin.get(Area.UUID), areaUuid);
		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationData.AGE_GROUP), ageGroup));
		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), criteria.getCampaign().getUuid()));
		Predicate filter_ = CriteriaBuilderHelper.and(cb, campaignFilter, cb.equal(root.get("selected"), true));
		Predicate selectedFilter = cb.and(cb.equal(root.get(PopulationData.SELECTED), true));

		cq.where(areaFilter, ageFilter, filter_, selectedFilter);
		cq.select(root.get(PopulationData.POPULATION));
		// System.out.println("DEBUGGER 5678ijhyuio
		// ___xxxxxcccccc____TOtalpopulation____________________________
		// "+SQLExtractor.from(em.createQuery(cq)));

		TypedQuery query = em.createQuery(cq);
		try {
			Integer totalPopulation = 0;
			for (Object i : query.getResultList()) {
				if (Objects.nonNull(i)) {
					totalPopulation = totalPopulation + (Integer) i;
				}
			}
			return totalPopulation;
		} catch (NoResultException e) {
			return null;
		}
	}

	public Integer getAreaPopulationParent(String notneeded, AgeGroup ageGroup, CampaignDiagramCriteria criteria) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);

		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationData.AGE_GROUP), ageGroup));
		System.out.println("DEBUGGERcccccccccooooo: -----============ " + criteria.getCampaign());
		if (criteria.getCampaign() != null) {

			System.out.println("DEBUGGERcccccccccbbbb: -----============ " + criteria.getCampaign().getUuid());
			// campaignService
			Predicate filterx = CriteriaBuilderHelper.and(cb, ageFilter,
					cb.equal(root.join(PopulationData.CAMPAIGN, JoinType.LEFT).get(Campaign.UUID),
							criteria.getCampaign().getUuid()),
					cb.equal(root.get("selected"), true));

			cq.where(filterx);
		} else {
			cq.where(ageFilter);
		}

		cq.select(root.get(PopulationData.POPULATION));
		System.out.println("DEBUGGER 56 TOtalpopulation______: " + SQLExtractor.from(em.createQuery(cq)));

		TypedQuery query = em.createQuery(cq);
		try {
			Integer totalPopulation = 0;
			for (Object i : query.getResultList()) {
				if (Objects.nonNull(i)) {
					totalPopulation = totalPopulation + (Integer) i;
				}
			}
			return totalPopulation;
		} catch (NoResultException e) {
			return null;
		}
	}

	public Integer getAreaPopulationParentSelect(String notneeded, AgeGroup ageGroup,
			CampaignDiagramCriteria criteria) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
		Root<PopulationData> root = cq.from(PopulationData.class);

		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationData.AGE_GROUP), ageGroup));
		System.out.println("DEBUGGERcccccccccooooo: -----============ " + criteria.getCampaign());
		if (criteria.getCampaign() != null) {

			System.out.println("DEBUGGERcccccccccbbbb: -----============ " + criteria.getCampaign().getUuid());
			// campaignService
			Predicate filterx = CriteriaBuilderHelper.and(cb, ageFilter,
					cb.equal(root.join(PopulationData.CAMPAIGN, JoinType.LEFT).get(Campaign.UUID),
							criteria.getCampaign().getUuid()),
					cb.equal(root.get("selected"), true));
			Predicate selectedFilter = cb.and(cb.equal(root.get(PopulationData.SELECTED), true));

			cq.where(filterx, selectedFilter);
		} else {
			cq.where(ageFilter);
		}

		cq.select(root.get(PopulationData.POPULATION));
		System.out.println("DEBUGGER 56 TOtalpopulation______: " + SQLExtractor.from(em.createQuery(cq)));

		TypedQuery query = em.createQuery(cq);
		try {
			Integer totalPopulation = 0;
			for (Object i : query.getResultList()) {
				if (Objects.nonNull(i)) {
					totalPopulation = totalPopulation + (Integer) i;
				}
			}
			return totalPopulation;
		} catch (NoResultException e) {
			return null;
		}
	}

	private void validate(PopulationDataDryRunDto populationData) throws ValidationRuntimeException {

		if (populationData.getRegion() == null) {
			throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.validRegion));
		}
	}

//	private void validate(PopulationDataFauxDto populationData) throws ValidationRuntimeException {
//
//		if (populationData.getRegion() == null) {
//			throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.validRegion));
//		}
//	}

	public PopulationDataDryRun fromDto(@NotNull PopulationDataDryRunDto source, boolean checkChangeDate) {

		PopulationDataDryRun target = DtoHelper.fillOrBuildEntity(source, populationDataDryRunService.getByUuid(source.getUuid()),
				PopulationDataDryRun::new, checkChangeDate);

		target.setRegion(regionService.getByReferenceDto(source.getRegion()));
		target.setDistrict(districtService.getByReferenceDto(source.getDistrict()));
		target.setCommunity(communityService.getByReferenceDto(source.getCommunity()));
		target.setCampaign(campaignService.getByReferenceDto(source.getCampaign()));
		target.setAgeGroup(source.getAgeGroup());
		target.setSex(source.getSex());
		target.setPopulation(source.getPopulation());
		target.setCollectionDate(source.getCollectionDate());
		target.setModality(source.getModality());
		target.setDistrictStatus(source.getDistrictStatus());

		return target;
	}


	public static PopulationDataDryRunDto toDto(PopulationDataDryRun source) {

		if (source == null) {
			return null;
		}
		PopulationDataDryRunDto target = new PopulationDataDryRunDto();
		DtoHelper.fillDto(target, source);

		target.setRegion(RegionFacadeEjb.toReferenceDto(source.getRegion()));
		target.setDistrict(DistrictFacadeEjb.toReferenceDto(source.getDistrict()));
		target.setCommunity(CommunityFacadeEjb.toReferenceDto(source.getCommunity()));
		target.setCampaign(CampaignFacadeEjb.toReferenceDto(source.getCampaign()));
		target.setAgeGroup(source.getAgeGroup());
		target.setSex(source.getSex());
		target.setPopulation(source.getPopulation());
		target.setCollectionDate(source.getCollectionDate());
		target.setModality(source.getModality());
		target.setDistrictStatus(source.getDistrictStatus());


		return target;
	}

	@LocalBean
	@Stateless
	public static class PopulationDataDryRunFacadeEjbLocal extends PopulationDataDryRunFacadeEjb {

	}

	@Override
	public void savePopulationList(Set<PopulationDataDryRunDto> savePopulationList) {
		if (savePopulationList.size() > 0) {
			String buildsql = "";
			String buildsql_ = "";
			for (PopulationDataDryRunDto dfc : savePopulationList) {
				// System.out.println("=============== ");
				buildsql = buildsql + districtService.getByUuid(dfc.getDistrict().getUuid()).getId() + ", ";
				buildsql_ = campaignService.getByUuid(dfc.getCampaign().getUuid()).getId() + "";
				// dfc.getCampaign().getUuid()
			}
			buildsql = buildsql + "#";

			{

				String sqlstatemtnt = "update populationdata set selected = 'false' where campaign_id = " + buildsql_
						+ ";";
				System.out.println(sqlstatemtnt);
				em.createNativeQuery(sqlstatemtnt).executeUpdate();
			}

			{
				String sqlstatemtnt_ = "update populationdata set selected = 'true' where campaign_id = " + buildsql_
						+ "" + " and district_id in (" + buildsql.replace(", #", "") + ");";

				System.out.println(sqlstatemtnt_);
				em.createNativeQuery(sqlstatemtnt_).executeUpdate();
			}

			// TODO Auto-generated method stub
		}

	}

	@Override
	public Integer getDistrictPopulationByUuidAndAgeGroup(String districtUuid, String campaignUuid, String ageGroup) {
		// TODO Auto-generated method stub

		final String joinBuilder = "select population \n" + "from PopulationData population \n"
				+ "inner join campaigns campaign on population.campaign_id=campaign.id \n"
				+ "inner join District district on population.district_id=district.id \n" + "where campaign.uuid= '"
				+ campaignUuid + "' and district.uuid='" + districtUuid + "' and population.ageGroup='" + ageGroup
				+ "';";

//		System.out.println(districtUuid  +  campaignUuid +  ageGroup + "Credentials from backend OOOPPPPPP" +joinBuilder);

		try {
			return (Integer) em.createNativeQuery(joinBuilder).getSingleResult();

		} catch (NoResultException e) {
			return null;
		}

	}

	@Override
	public String getDistrictStatusByCampaign(String districtUuid, String campaignUuid, String ageGroup) {
		// TODO Auto-generated method stub
		final String joinBuilder = "select districtstatus \n" + "from PopulationData population \n"
				+ "inner join campaigns campaign on population.campaign_id=campaign.id \n"
				+ "inner join District district on population.district_id=district.id \n" + "where campaign.uuid= '"
				+ campaignUuid + "' and district.uuid='" + districtUuid + "' and population.ageGroup='" + ageGroup
				+ "';";
		try {
			return (String) em.createNativeQuery(joinBuilder).getSingleResult();

		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	public String getDistrictModalityByUuidAndCampaignAndAgeGroup(String districtUuid, String campaignUuid,
			String ageGroup) {
		// TODO Auto-generated method stub
		final String joinBuilder = "select modality \n" + "from PopulationData population \n"
				+ "inner join campaigns campaign on population.campaign_id=campaign.id \n"
				+ "inner join District district on population.district_id=district.id \n" + "where campaign.uuid= '"
				+ campaignUuid + "' and district.uuid='" + districtUuid + "' and population.ageGroup='" + ageGroup
				+ "';";

		try {
			return (String) em.createNativeQuery(joinBuilder).getSingleResult();

		} catch (NoResultException e) {
			return null;
		}
	}
//	@Override
//	public void truncateDryRunTable() {
//		
//		System.out.println("11111----------------------------------------------Credentials from backend OOOPPPPPP" );
//
//		final String joinBuilder = "truncate table populationdatadryrun;";
//		
//		
//		  Query query = em.createNativeQuery(joinBuilder);
//
//		    // Execute the query
//		    query.executeUpdate();
////		em.createNativeQuery(joinBuilder);
//
//	}

	@Override
	public List<PopulationDataDryRunDto> getDistrictModalityByUUIDsandCampaignUUIdAndAgeGroup(String districtUuid,
			String campaignUuid, AgeGroup agegroup) {
		// TODO Auto-generated method stub
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PopulationDataDryRun> cq = cb.createQuery(PopulationDataDryRun.class);
		Root<PopulationDataDryRun> root = cq.from(PopulationDataDryRun.class);
		Join<PopulationDataDryRun, Campaign> campaignJoin = root.join(PopulationDataDryRun.CAMPAIGN);
		Join<PopulationDataDryRun, District> districtJoin = root.join(PopulationDataDryRun.DISTRICT);

		System.out.println(districtUuid + "1111zzzzzzDEBUGGER 5678ijhyuioYYYYYY" + campaignUuid  + "1111zzzzzzDEBUGGER 5678ijhyuioYYYYYY" + agegroup);

		Predicate campaignFilter = cb.and(cb.equal(campaignJoin.get(Campaign.UUID), campaignUuid));
		Predicate districtFilter = cb.and(cb.equal(districtJoin.get(District.UUID), districtUuid));
		Predicate ageFilter = cb.and(cb.equal(root.get(PopulationDataDryRun.AGE_GROUP), agegroup));

		cq.where(campaignFilter, districtFilter, ageFilter);

//		 System.out.println(//"resultData - "+ resultData.toString());
//		 "DUMBGFyyresultData - "+SQLExtractor.from(seriesDataQuery));

		System.out.println("1111zzzzzzDEBUGGER 5678ijhyuioYYYYYY" + SQLExtractor.from(em.createQuery(cq)));

		return em.createQuery(cq).getResultStream().map(populationData -> toDto(populationData))
				.collect(Collectors.toList());
	}
	



	@Override
	public boolean checkDuplicatePopulationData(String districtExternalID, String AgeGroup,
			String campaignUUId) {
		// TODO Auto-generated method stub
		
		final String joinBuilder = "SELECT COUNT(*) FROM populationdatadryrun WHERE district_id = " + districtExternalID +" AND agegroup = " + AgeGroup + " AND campaign_id = " + campaignUUId + ";";

		System.out.println(joinBuilder + "Credentials from backend OOOPPPPPP" );

		try {
			return true ;//(boolean) em.createNativeQuery(joinBuilder).getSingleResult();

		} catch (NoResultException e) {
			return false;
		}
	
	}

	@Override
	public void truncateDryRunTable() {
		// TODO Auto-generated method stub
		
		String query = "select * from users where username is not null";
		
	}

	
	


	
	

}