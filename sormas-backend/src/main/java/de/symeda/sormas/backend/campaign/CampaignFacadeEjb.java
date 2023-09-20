package de.symeda.sormas.backend.campaign;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignCriteria;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignFacade;
import de.symeda.sormas.api.campaign.CampaignIndexDto;
import de.symeda.sormas.api.campaign.CampaignLogDto;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDashboardElement;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramDefinitionDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.SortProperty;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.backend.campaign.diagram.CampaignDiagramDefinitionFacadeEjb;
import de.symeda.sormas.backend.campaign.form.CampaignFormMetaService;
import de.symeda.sormas.backend.common.AbstractDomainObject;
import de.symeda.sormas.backend.common.CriteriaBuilderHelper;
import de.symeda.sormas.backend.infrastructure.PopulationData;
import de.symeda.sormas.backend.infrastructure.PopulationDataService;
import de.symeda.sormas.backend.infrastructure.area.Area;
import de.symeda.sormas.backend.infrastructure.area.AreaFacadeEjb;
import de.symeda.sormas.backend.infrastructure.area.AreaService;
import de.symeda.sormas.backend.infrastructure.community.Community;
import de.symeda.sormas.backend.infrastructure.community.CommunityFacadeEjb;
import de.symeda.sormas.backend.infrastructure.community.CommunityService;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.district.DistrictFacadeEjb;
import de.symeda.sormas.backend.infrastructure.district.DistrictService;
import de.symeda.sormas.backend.infrastructure.region.Region;
import de.symeda.sormas.backend.infrastructure.region.RegionFacadeEjb;
import de.symeda.sormas.backend.infrastructure.region.RegionService;
import de.symeda.sormas.backend.user.User;
import de.symeda.sormas.backend.user.UserFacadeEjb;
import de.symeda.sormas.backend.user.UserRoleConfigFacadeEjb.UserRoleConfigFacadeEjbLocal;
import de.symeda.sormas.backend.user.UserService;
import de.symeda.sormas.backend.util.DtoHelper;
import de.symeda.sormas.backend.util.ModelConstants;
import de.symeda.sormas.backend.util.QueryHelper;

@Stateless(name = "CampaignFacade")
public class CampaignFacadeEjb implements CampaignFacade {

	@PersistenceContext(unitName = ModelConstants.PERSISTENCE_UNIT_NAME)
	private EntityManager em;

	@EJB
	private CampaignService campaignService;
	@EJB
	private CampaignLogService campaignLogService;
	@EJB
	private CampaignFormMetaService campaignFormMetaService;
	@EJB
	private UserService userService;
	@EJB
	private UserRoleConfigFacadeEjbLocal userRoleConfigFacade;
	@EJB
	private CampaignDiagramDefinitionFacadeEjb.CampaignDiagramDefinitionFacadeEjbLocal campaignDiagramDefinitionFacade;
	@EJB
	private AreaService areaService;
	@EJB
	private RegionService regionService;
	@EJB
	private DistrictService districtService;
	@EJB
	private CommunityService communityService;
	@EJB
	private PopulationDataService popService;
	@EJB
	private UserFacadeEjb.UserFacadeEjbLocal userServiceEBJ;

	@Override
	public List<CampaignIndexDto> getIndexList(CampaignCriteria campaignCriteria, Integer first, Integer max,
			List<SortProperty> sortProperties) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CampaignIndexDto> cq = cb.createQuery(CampaignIndexDto.class);
		Root<Campaign> campaign = cq.from(Campaign.class);

		cq.multiselect(campaign.get(Campaign.UUID), campaign.get(Campaign.NAME), campaign.get(Campaign.CLOSEOPEN),
				campaign.get(Campaign.ROUND), campaign.get(Campaign.CAMPAIGN_YEAR), campaign.get(Campaign.START_DATE),
				campaign.get(Campaign.END_DATE), campaign.get(Campaign.ARCHIVED));

		Predicate filter = campaignService.createUserFilter(cb, cq, campaign);

		if (campaignCriteria != null) {
			Predicate criteriaFilter = campaignService.buildCriteriaFilter(campaignCriteria, cb, campaign);
			filter = CriteriaBuilderHelper.and(cb, filter, criteriaFilter);
		}

		cq.where(filter);

		if (sortProperties != null && sortProperties.size() > 0) {
			List<Order> order = new ArrayList<Order>(sortProperties.size());
			for (SortProperty sortProperty : sortProperties) {
				System.out.println("jgcgjcjgcdgjcxjgcxgjcjPROPERTY NAMEgcjggjcjgc jcj" + sortProperty.propertyName);
				Expression<?> expression;
				switch (sortProperty.propertyName) {
				case CampaignIndexDto.UUID:
				case CampaignIndexDto.ROUND:
				case CampaignIndexDto.CAMPAIGN_YEAR:
				case CampaignIndexDto.NAME:
				case CampaignIndexDto.START_DATE:
				case CampaignIndexDto.END_DATE:
					expression = campaign.get(sortProperty.propertyName);
					break;
				case CampaignIndexDto.CAMPAIGN_STATUS:
					expression = campaign.get(Campaign.CLOSEOPEN);
					break;
				case CampaignIndexDto.ARCHIVE:
					expression = campaign.get(Campaign.ARCHIVED);
					break;
				default:
					throw new IllegalArgumentException(sortProperty.propertyName);
				}
				order.add(sortProperty.ascending ? cb.asc(expression) : cb.desc(expression));
			}
			cq.orderBy(order);
		} else {
			cq.orderBy(cb.desc(campaign.get(Campaign.START_DATE)));
		}

		return QueryHelper.getResultList(em, cq, first, max);
	}

	@Override
	public List<CampaignReferenceDto> getAllActiveCampaignsAsReference() {
		return campaignService.getAll().stream().filter(c -> !c.isDeleted() && !c.isArchived())
				.map(CampaignFacadeEjb::toReferenceDtoYear).collect(Collectors.toList());
	}

	@Override
	public CampaignReferenceDto getLastStartedCampaign() {

		final CriteriaBuilder cb = em.getCriteriaBuilder();
		final CriteriaQuery<Campaign> query = cb.createQuery(Campaign.class);
		final Root<Campaign> from = query.from(Campaign.class);
		query.select(from);
		query.where(cb.and(campaignService.createActiveCampaignsFilter(cb, from),
				cb.lessThanOrEqualTo(from.get(Campaign.START_DATE), new Date())));
		query.orderBy(cb.desc(from.get(Campaign.START_DATE)));

		// //System.out.println(new Date() + " DEBUGGER r567ujhgty8ijyu8dfrf
		// "+SQLExtractor.from(em.createQuery(query)));

		final TypedQuery<Campaign> q = em.createQuery(query);
		final Campaign lastStartedCampaign = q.getResultList().stream().findFirst().orElse(null);

		return toReferenceDtoYear(lastStartedCampaign);
	}

	@Override
	public long count(CampaignCriteria campaignCriteria) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<Campaign> campaign = cq.from(Campaign.class);

		Predicate filter = campaignService.createUserFilter(cb, cq, campaign);

		if (campaignCriteria != null) {
			Predicate criteriaFilter = campaignService.buildCriteriaFilter(campaignCriteria, cb, campaign);
			filter = CriteriaBuilderHelper.and(cb, filter, criteriaFilter);
		}

		cq.where(filter);
		cq.select(cb.count(campaign));
		return em.createQuery(cq).getSingleResult();
	}

	@Override
	public CampaignDto saveCampaign(@Valid CampaignDto dto) {
System.out.println(dto + "from the campaign facade when its trying to save ");
		Campaign campaign = fromDto(dto, true);
		campaignService.ensurePersisted(campaign);
		return toDto(campaign);
	}

	@Override
	public CampaignLogDto saveAuditLog(CampaignLogDto campaignLogDto) {
		campaignLogDto.setCreatingUser(userServiceEBJ.getCurrentUser());
		CampaignLog campaignLog = fromDto(campaignLogDto);
		campaignLog.setCreatingUser(userService.getCurrentUser());
		campaignLogService.ensurePersisted(campaignLog);
		return toLogDto(campaignLog);
	}

//	@Override
//	public List<CampaignLogDto> getAuditLog(CampaignReferenceDto camp) {
//
//		String qr = "select c.action_logged, c.last_updated, ca.name, ur.username from campaignlog c\n"
//				+ "left outer join campaigns ca on c.campaign = ca.id\n"
//				+ "left outer join users ur on c.creatinguser = ur.id \n"
//				+ "where ca.uuid = '"+camp.getUuid()+"';";
//
//		Query seriesDataQuery = em.createNativeQuery(qr);
//
//		List<CampaignLogDto> resultData = new ArrayList<>();
//
//		@SuppressWarnings("unchecked")
//		List<Object[]> resultList = seriesDataQuery.getResultList();
//
//		System.out.println("starting....");
//
//		resultData
//				.addAll(resultList.stream()
//						.map((result) -> new CampaignLogDto((String) result[0].toString(), (Date) result[1],
//								(String) result[2].toString(), (String) result[3].toString()))
//						.collect(Collectors.toList()));
//
//		return resultData;
//	}
	@Override
	public List<CampaignLogDto> getAuditLog(CampaignReferenceDto camp) {

		String qr = "select c.action_logged, c.lastupdated, ca.name, ur.username from campaignlog c\n"
				+ "left outer join campaigns ca on c.campaign_id = ca.id\n"
				+ "left outer join users ur on c.creatinguser_id = ur.id \n"
				+ "where ca.uuid = '"+camp.getUuid()+"';";

		Query seriesDataQuery = em.createNativeQuery(qr);

		List<CampaignLogDto> resultData = new ArrayList<>();

		@SuppressWarnings("unchecked")
		List<Object[]> resultList = seriesDataQuery.getResultList();

		System.out.println("starting....");

		resultData
				.addAll(resultList.stream()
						.map((result) -> new CampaignLogDto((String) result[0].toString(), (Date) result[1],
								(String) result[2].toString(), (String) result[3].toString()))
						.collect(Collectors.toList()));

		return resultData;
	}

	public CampaignLog fromDto(@NotNull CampaignLogDto source) {

		CampaignLog target = new CampaignLog();

		target.setCreatingUser(userService.getByUuid(source.getCreatingUser().getUuid()));
		target.setAction(source.getAction());
		target.setCampaign(campaignService.getByUuid(source.getCampaign().getUuid()));

		return target;
	}

	public CampaignLogDto toLogDto(CampaignLog source) {

		if (source == null) {
			return null;
		}
		CampaignLogDto target = new CampaignLogDto();

		target.setCreatingUser(userServiceEBJ.getByUuid(source.getCreatingUser().getUuid()));
		target.setAction(source.getAction());
		target.setCampaign(getByUuid(source.getCampaign().getUuid()));
		return target;
	}

	public Campaign fromDto(@NotNull CampaignDto source, boolean checkChangeDate) {
		
		System.out.println(source + " source fromdtoooooooo");
		validate(source);

		Campaign target = DtoHelper.fillOrBuildEntity(source, campaignService.getByUuid(source.getUuid()),
				Campaign::new, checkChangeDate);

		target.setCreatingUser(userService.getByReferenceDto(source.getCreatingUser()));
		target.setDescription(source.getDescription());
		target.setEndDate(source.getEndDate());
		target.setName(source.getName());
		target.setRound(source.getRound());
		target.setCampaignYear(source.getCampaignYear());
		target.setStartDate(source.getStartDate());

		final Set<AreaReferenceDto> areas = source.getAreas();
		if (!CollectionUtils.isEmpty(areas)) {
			target.setAreas(areas.stream().map(e -> areaService.getByUuid(e.getUuid())).collect(Collectors.toSet()));
		}

		final Set<RegionReferenceDto> region = source.getRegion();
		if (!CollectionUtils.isEmpty(region)) {
			target.setRegion(
					region.stream().map(e -> regionService.getByUuid(e.getUuid())).collect(Collectors.toSet()));
		}

		final Set<DistrictReferenceDto> district = source.getDistricts();
		if (!CollectionUtils.isEmpty(district)) {
			target.setDistricts(
					district.stream().map(e -> districtService.getByUuid(e.getUuid())).collect(Collectors.toSet()));
		}

		final Set<CommunityReferenceDto> community = source.getCommunity();
		if (!CollectionUtils.isEmpty(community)) {
			target.setCommunity(
					community.stream().map(e -> communityService.getByUuid(e.getUuid())).collect(Collectors.toSet()));
		}

		final Set<CampaignFormMetaReferenceDto> campaignFormMetas = source.getCampaignFormMetas(); // Campaign data
		if (!CollectionUtils.isEmpty(campaignFormMetas)) {
			target.setCampaignFormMetas(
					campaignFormMetas.stream().map(campaignFormMetaReferenceDto -> campaignFormMetaService
							.getByUuid(campaignFormMetaReferenceDto.getUuid())).collect(Collectors.toSet()));
		}
		target.setDashboardElements(source.getCampaignDashboardElements());// .stream().filter(e ->
																			// e.getDiagramId().equals("")));
		return target;

	}

	public void validate(CampaignReferenceDto campaignReferenceDto, String formPhase) {
		validate(getByUuid(campaignReferenceDto.getUuid()), formPhase);

	}

	protected void validate(CampaignDto campaignDto, String formPhase) {
		final List<CampaignDashboardElement> nonfilteredList = campaignDto.getCampaignDashboardElements();

		List<CampaignDashboardElement> campaignDashboardElements;

		campaignDashboardElements = nonfilteredList.stream().filter(p -> p.getTabId() == "")
				.collect(Collectors.toList());
		// .filter(phaseFilter ->
		// phaseFilter.contains(formPhase)).collect(Collectors.toList());

		if (campaignDashboardElements != null) {

			final Map<String, Boolean> oneSubTabIsNotNullOrEmptyMap = new HashMap<>();

			for (CampaignDashboardElement cde : campaignDashboardElements) {
				final String diagramId = cde.getDiagramId();
				if (diagramId == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.DIAGRAM_ID, campaignDto.getName()));
				} else if (!campaignDiagramDefinitionFacade.exists(diagramId)) {
					throw new ValidationRuntimeException(I18nProperties.getValidationError(
							Validations.campaignDashboardChartIdDoesNotExist, diagramId, campaignDto.getName()));
				}

				if (cde.getTabId() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.TAB_ID, campaignDto.getName()));
				}

				if (cde.getSubTabId() == null || cde.getSubTabId().isEmpty()) {
					if (oneSubTabIsNotNullOrEmptyMap.containsKey(cde.getTabId())
							&& oneSubTabIsNotNullOrEmptyMap.get(cde.getTabId())) {
						throw new ValidationRuntimeException(
								I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
										CampaignDashboardElement.SUB_TAB_ID, campaignDto.getName()));
					}
					oneSubTabIsNotNullOrEmptyMap.put(cde.getTabId(), false);
				} else {
					if (oneSubTabIsNotNullOrEmptyMap.containsKey(cde.getTabId())
							&& !oneSubTabIsNotNullOrEmptyMap.get(cde.getTabId())) {
						throw new ValidationRuntimeException(
								I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
										CampaignDashboardElement.SUB_TAB_ID, campaignDto.getName()));
					}
					oneSubTabIsNotNullOrEmptyMap.put(cde.getTabId(), true);
				}

				if (cde.getOrder() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.ORDER, campaignDto.getName()));
				}

				if (cde.getHeight() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.HEIGHT, campaignDto.getName()));
				}

				if (cde.getWidth() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.WIDTH, campaignDto.getName()));
				}
			}

			campaignDto.getCampaignFormMetas().forEach(campaignFormMetaReferenceDto -> {
				if (campaignFormMetaReferenceDto == null || campaignFormMetaReferenceDto.getUuid() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardDataFormValueNull,
									CampaignDto.CAMPAIGN_FORM_METAS, campaignDto.getName()));
				}
			});
		}
	}

	public void validate(CampaignReferenceDto campaignReferenceDto) {
		validate(getByUuid(campaignReferenceDto.getUuid()));
	}

	protected void validate(CampaignDto campaignDto) {
		final List<CampaignDashboardElement> campaignDashboardElements = campaignDto.getCampaignDashboardElements();

		if (campaignDashboardElements != null) {

			final Map<String, Boolean> oneSubTabIsNotNullOrEmptyMap = new HashMap<>();

			for (CampaignDashboardElement cde : campaignDashboardElements) {
				final String diagramId = cde.getDiagramId();
				if (diagramId == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.DIAGRAM_ID, campaignDto.getName()));
				} else if (!campaignDiagramDefinitionFacade.exists(diagramId)) {
					throw new ValidationRuntimeException(I18nProperties.getValidationError(
							Validations.campaignDashboardChartIdDoesNotExist, diagramId, campaignDto.getName()));
				}

				if (cde.getTabId() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.TAB_ID, campaignDto.getName()));
				}

				if (cde.getSubTabId() == null || cde.getSubTabId().isEmpty()) {
					if (oneSubTabIsNotNullOrEmptyMap.containsKey(cde.getTabId())
							&& oneSubTabIsNotNullOrEmptyMap.get(cde.getTabId())) {
						throw new ValidationRuntimeException(
								I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
										CampaignDashboardElement.SUB_TAB_ID, campaignDto.getName()));
					}
					oneSubTabIsNotNullOrEmptyMap.put(cde.getTabId(), false);
				} else {
					if (oneSubTabIsNotNullOrEmptyMap.containsKey(cde.getTabId())
							&& !oneSubTabIsNotNullOrEmptyMap.get(cde.getTabId())) {
						throw new ValidationRuntimeException(
								I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
										CampaignDashboardElement.SUB_TAB_ID, campaignDto.getName()));
					}
					oneSubTabIsNotNullOrEmptyMap.put(cde.getTabId(), true);
				}

				if (cde.getOrder() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.ORDER, campaignDto.getName()));
				}

				if (cde.getHeight() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.HEIGHT, campaignDto.getName()));
				}

				if (cde.getWidth() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardChartValueNull,
									CampaignDashboardElement.WIDTH, campaignDto.getName()));
				}
			}

			campaignDto.getCampaignFormMetas().forEach(campaignFormMetaReferenceDto -> {
				if (campaignFormMetaReferenceDto == null || campaignFormMetaReferenceDto.getUuid() == null) {
					throw new ValidationRuntimeException(
							I18nProperties.getValidationError(Validations.campaignDashboardDataFormValueNull,
									CampaignDto.CAMPAIGN_FORM_METAS, campaignDto.getName()));
				}
			});
		}
	}

	public CampaignDto toDto(Campaign source) {

		if (source == null) {
			return null;
		}

		CampaignDto target = new CampaignDto();
		DtoHelper.fillDto(target, source);
		//// System.out.println("++++++++++++++++
		//// "+UserFacadeEjb.toReferenceDto(source.getCreatingUser()));
		target.setCreatingUser(UserFacadeEjb.toReferenceDto(source.getCreatingUser()));
		target.setDescription(source.getDescription());
		target.setEndDate(source.getEndDate());
		target.setName(source.getName());
		target.setRound(source.getRound());
		target.setCampaignYear(source.getCampaignYear());
		target.setStartDate(source.getStartDate());
		target.setCampaignFormMetas(source.getCampaignFormMetas().stream()
				.map(campaignFormMeta -> campaignFormMeta.toReference()).collect(Collectors.toSet()));
		target.setAreas(AreaFacadeEjb.toReferenceDto(new HashSet<Area>(source.getAreas())));
		target.setRegion(RegionFacadeEjb.toReferenceDto(new HashSet<Region>(source.getRegion())));
		target.setDistricts(DistrictFacadeEjb.toReferenceDto(new HashSet<District>(source.getDistricts())));
		target.setCommunity(CommunityFacadeEjb.toReferenceDto(new HashSet<Community>(source.getCommunity())));
		target.setCampaignDashboardElements(source.getDashboardElements());
		target.setPublished(source.isPublished());

		return target;
	}

	@Override
	public CampaignDto getByUuid(String uuid) {
		//// System.out.println("dddddddddddddddddddddddddddd111111111111111111111112222222222222222222222222");
		return toDto(campaignService.getByUuid(uuid));
	}

	@Override
	public List<CampaignDashboardElement> getCampaignDashboardElements(String campaignUuid, String formType) {

		final List<CampaignDashboardElement> result = new ArrayList<>();
		if (campaignUuid != null && formType != null) {
			final Campaign campaign = campaignService.getByUuid(campaignUuid);

			final List<CampaignDashboardElement> dashboardElements = campaign.getDashboardElements();
			final List<CampaignDashboardElement> dashboardElements_1;
			final List<CampaignDashboardElement> dashboardElements_2;

			if (dashboardElements != null) {
				dashboardElements_1 = dashboardElements.stream().filter(p -> p.getPhase() != null)
						.collect(Collectors.toList());
				dashboardElements_2 = dashboardElements_1.stream().filter(p -> p.getPhase().equals(formType))
						.collect(Collectors.toList());
				result.addAll(dashboardElements_2);
			}

		} else if (campaignUuid == null && formType != null) {

			campaignService.getAllActive().forEach(campaign -> {

				final List<CampaignDiagramDefinitionDto> campaignDiagramDefinitionDto_s = campaignDiagramDefinitionFacade
						.getAll();

				List<CampaignDashboardElement> dashboardElementsx = campaign.getDashboardElements();// .stream().filter(e
																									// -> e.getPhase()
																									// !=
																									// null).collect(Collectors.toList());
				// //System.out.println(campaignDiagramDefinitionDto_s+"
				// +++++++++++++++++++++++++=============== "+formType);

				CampaignDashboardElement campaignDashboardElement = new CampaignDashboardElement();

				List<CampaignDashboardElement> dashboardElements = new ArrayList();

				// //System.out.println(formType+" +++++++++++++++++++++++"+
				// campaignDiagramDefinitionDto_s.stream().filter(e ->
				// e.getFormType().equalsIgnoreCase(formType)).collect(Collectors.toList()));

				final List<CampaignDiagramDefinitionDto> campaignDiagramDefinitionDtos = campaignDiagramDefinitionDto_s
						.stream().filter(e -> e.getFormType().equalsIgnoreCase(formType)).collect(Collectors.toList());

				// //System.out.println(formType+"
				// +++++++++++++++++++++++++====================== bbb ===========
				// "+campaignDiagramDefinitionDtos);

				for (int i = 0; i < campaignDiagramDefinitionDtos.size(); i++) {

					// //System.out.println("++++++++++++++++0000000
					// "+campaignDiagramDefinitionDtos.get(i).getFormType());

					campaignDashboardElement.setDiagramId(campaignDiagramDefinitionDtos.get(i).getDiagramId());
					campaignDashboardElement.setPhase(campaignDiagramDefinitionDtos.get(i).getFormType());

					// check for null
					if (!dashboardElementsx.isEmpty()) {
						if (dashboardElementsx.size() > i) {
							campaignDashboardElement.setTabId(dashboardElementsx.get(i).getTabId() == null ? ""
									: dashboardElementsx.get(i).getTabId());
							campaignDashboardElement.setSubTabId(dashboardElementsx.get(i).getSubTabId() == null ? ""
									: dashboardElementsx.get(i).getSubTabId());
						} else {
							campaignDashboardElement.setTabId("");
							campaignDashboardElement.setSubTabId("");
						}
					} else {
						campaignDashboardElement.setTabId("");
						campaignDashboardElement.setSubTabId("");
					}
					//
					dashboardElements.add(campaignDashboardElement);
				}

				if (dashboardElements != null) {
					result.addAll(dashboardElements);
				}
			});

		} else {
			campaignService.getAllActive().forEach(campaign -> {
				final List<CampaignDashboardElement> dashboardElements = campaign.getDashboardElements().stream()
						.filter(e -> e.getPhase() != null).collect(Collectors.toList());
				if (dashboardElements != null) {
					result.addAll(dashboardElements);
				}
			});
		}

		result.forEach(cde -> {
			if (cde.getTabId() == null) {
				cde.setTabId(StringUtils.EMPTY);
			}
			if (cde.getSubTabId() == null) {
				cde.setSubTabId(StringUtils.EMPTY);
			}
			if (cde.getOrder() == null) {
				cde.setOrder(0);
			}
			if (cde.getHeight() == null) {
				cde.setHeight(50);
			}
			if (cde.getWidth() == null) {
				cde.setWidth(50);
			}
		});
		return result.stream().sorted(Comparator.comparingInt(CampaignDashboardElement::getOrder))
				.collect(Collectors.toList());
	}

	@Override
	public boolean isArchived(String uuid) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<Campaign> from = cq.from(Campaign.class);

		// Workaround for probable bug in Eclipse Link/Postgre that throws a
		// NoResultException when trying to
		// query for a true Boolean result
		cq.where(cb.and(cb.equal(from.get(Campaign.ARCHIVED), true),
				cb.equal(from.get(AbstractDomainObject.UUID), uuid)));
		cq.select(cb.count(from));
		long count = em.createQuery(cq).getSingleResult();
		return count > 0;
	}

	@Override
	public boolean isClosedd(String uuid) {
		String cdvv = "select openandclose from campaigns where uuid = '" + uuid + "' and openandclose = false";

		List count = em.createNativeQuery(cdvv).getResultList();
		// System.out.println(cdvv +" ++++++++++++++++ "+count.size());
		return count.size() > 0;
	}

	@Override
	public boolean isPublished(String uuid) {
		String cdvv = "select published from campaigns where uuid = '" + uuid + "' and published = false";

		List count = em.createNativeQuery(cdvv).getResultList();
		// System.out.println(cdvv +" ++++++++++++++++ "+count.size());
		return count.size() > 0;
	}

	@Override
	public void deleteCampaign(String campaignUuid) {

		User user = userService.getCurrentUser();
		if (!userRoleConfigFacade
				.getEffectiveUserRights(user.getUserRoles().toArray(new UserRole[user.getUserRoles().size()]))
				.contains(UserRight.CAMPAIGN_DELETE)) {
			throw new UnsupportedOperationException(
					I18nProperties.getString(Strings.entityUser) + " " + user.getUuid() + " is not allowed to delete "
							+ I18nProperties.getString(Strings.entityCampaigns).toLowerCase() + ".");
		}
		
		CampaignLogDto log = new CampaignLogDto();

		// logging audit
		if (campaignUuid != null) {
			log.setCampaign(getByUuid(campaignUuid));
			log.setAction("Deleted Campaign: " + log.getCampaign().getName());

		} 

		saveAuditLog(log);

		campaignService.delete(campaignService.getByUuid(campaignUuid));
	}

	@Override
	public String cloneCampaign(String campaignUuid, String userCreating) {

		User user = userService.getCurrentUser();
		if (!userRoleConfigFacade
				.getEffectiveUserRights(user.getUserRoles().toArray(new UserRole[user.getUserRoles().size()]))
				.contains(UserRight.CAMPAIGN_DELETE)) {
			throw new UnsupportedOperationException(I18nProperties.getString(Strings.entityUser) + " " + user.getUuid()
					+ " is not allowed to duplicate " + I18nProperties.getString(Strings.entityCampaigns).toLowerCase()
					+ ".");
		}
		String newUuid = campaignService.cloneForm(campaignService.getByUuid(campaignUuid), user.getId());

		List<PopulationData> popList = campaignService.clonePopulationData(campaignService.getByUuid(campaignUuid),
				null);
		final Campaign cmp = campaignService.getByUuid(newUuid);
		for (PopulationData popListx : popList) {
			PopulationData ppData = new PopulationData();
			ppData.setAgeGroup(popListx.getAgeGroup());
			ppData.setCampaign(cmp);
			ppData.setDistrict(popListx.getDistrict());
			ppData.setPopulation(popListx.getPopulation());
			ppData.setRegion(popListx.getRegion());
			ppData.setCollectionDate(popListx.getCollectionDate());
			ppData.setSelected(popListx.isSelected());
			em.persist(ppData);

		}
		CampaignLogDto log = new CampaignLogDto();

		// logging audit
		if (campaignUuid != null) {
			log.setCampaign(getByUuid(campaignUuid));
			log.setAction("Clone Campaign: " + log.getCampaign().getName());

		} 

		saveAuditLog(log);

		return newUuid;
	}

	@Override
	public void publishandUnPublishCampaign(String campaignUuid, boolean publishedandunpublishbutton) {

		User user = userService.getCurrentUser();
		if (!userRoleConfigFacade
				.getEffectiveUserRights(user.getUserRoles().toArray(new UserRole[user.getUserRoles().size()]))
				.contains(UserRight.CAMPAIGN_PUBLISH)) {
			throw new UnsupportedOperationException(I18nProperties.getString(Strings.entityUser) + " " + user.getUuid()
					+ " is not allowed to publish a campaign data  "
					+ I18nProperties.getString(Strings.entityCampaigns).toLowerCase() + ".");
		}
		CampaignLogDto log = new CampaignLogDto();

		// logging audit
		if (publishedandunpublishbutton) {
			log.setCampaign(getByUuid(campaignUuid));
			log.setAction("Publish Campaign: " + log.getCampaign().getName());

		} else {
			log.setCampaign(getByUuid(campaignUuid));
			log.setAction("Unpublish Campaign: " + log.getCampaign().getName());
		}

		saveAuditLog(log);

		campaignService.campaignPublish(campaignUuid, publishedandunpublishbutton);

	}

	@Override
	public void closeandOpenCampaign(String campaignUuid, boolean openandclosebutton) {
		
		campaignService.closeAndOpenForm(campaignUuid, openandclosebutton);

	}

	@Override
	public void archiveOrDearchiveCampaign(String campaignUuid, boolean archive) {

		Campaign campaign = campaignService.getByUuid(campaignUuid);
		campaign.setArchived(archive);
		
		CampaignLogDto log = new CampaignLogDto();

		// logging audit
		if (archive) {
			log.setCampaign(getByUuid(campaignUuid));
			log.setAction("Archive Campaign: " + log.getCampaign().getName());

		} else {
			log.setCampaign(getByUuid(campaignUuid));
			log.setAction("De-Archive Campaign: " + log.getCampaign().getName());
		}

		saveAuditLog(log);
		campaignService.ensurePersisted(campaign);
	}

	@Override
	public CampaignReferenceDto getReferenceByUuid(String uuid) {
		return toReferenceDtoYear(campaignService.getByUuid(uuid));
	}

	@Override
	public boolean exists(String uuid) {
		return campaignService.exists(uuid);
	}

	@Override
	public List<CampaignDto> getAllActive() {
		return campaignService.getAllActive().stream().map(campaignFormMeta -> toDto(campaignFormMeta))
				.collect(Collectors.toList());
	}

	@Override
	public List<CampaignDto> getAllAfter(Date date) {
		return campaignService.getAllAfter(date, userService.getCurrentUser()).stream()
				.map(campaignFormMeta -> toDto(campaignFormMeta)).collect(Collectors.toList());
	}

	@Override
	public List<CampaignDto> getByUuids(List<String> uuids) {
		return campaignService.getByUuids(uuids).stream().map(c -> toDto(c)).collect(Collectors.toList());
	}

	@Override
	public List<String> getAllActiveUuids() {
		if (userService.getCurrentUser() == null) {
			return Collections.emptyList();
		}

		return campaignService.getAllActiveUuids();
	}

	public static CampaignReferenceDto toReferenceDto(Campaign entity) {
		if (entity == null) {
			return null;
		}
		CampaignReferenceDto dto = new CampaignReferenceDto(entity.getUuid(), entity.toString());
		return dto;
	}

	public static CampaignReferenceDto toReferenceDtoYear(Campaign entity) {
		if (entity == null) {
			return null;
		}
		CampaignReferenceDto dto = new CampaignReferenceDto(entity.getUuid(), entity.toString(),
				entity.getCampaignYear());
		return dto;
	}

	@LocalBean
	@Stateless
	public static class CampaignFacadeEjbLocal extends CampaignFacadeEjb {
	}

}
