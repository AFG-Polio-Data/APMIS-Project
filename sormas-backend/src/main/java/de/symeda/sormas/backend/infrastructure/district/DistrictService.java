/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.backend.infrastructure.district;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.vladmihalcea.hibernate.type.util.SQLExtractor;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.country.CountryReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictCriteria;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.backend.common.AbstractInfrastructureAdoService;
import de.symeda.sormas.backend.common.CriteriaBuilderHelper;
import de.symeda.sormas.backend.infrastructure.region.Region;
import de.symeda.sormas.backend.infrastructure.country.Country;
import de.symeda.sormas.backend.infrastructure.country.CountryFacadeEjb.CountryFacadeEjbLocal;
import de.symeda.sormas.backend.infrastructure.area.Area;
import de.symeda.sormas.backend.infrastructure.community.Community;

@Stateless
@LocalBean
public class DistrictService extends AbstractInfrastructureAdoService<District> {

	@EJB
	private CountryFacadeEjbLocal countryFacade;

	public DistrictService() {
		super(District.class);
	}

	public List<District> getAllActiveByRegion(Region region) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<District> cq = cb.createQuery(getElementClass());
		Root<District> from = cq.from(getElementClass());
		cq.where(cb.and(createBasicFilter(cb, from), cb.equal(from.get(District.REGION), region)));
		cq.orderBy(cb.asc(from.get(District.NAME)));

		return em.createQuery(cq).getResultList();
	}

	public List<District> getAllActiveByArea(Area area) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<District> cq = cb.createQuery(getElementClass());
		Root<District> from = cq.from(getElementClass());

		Path<Area> regionJoin = from.join(District.REGION, JoinType.LEFT);
		cq.where(cb.and(createBasicFilter(cb, from), cb.equal(regionJoin.get(Region.AREA), area)));
		cq.orderBy(cb.asc(from.get(District.NAME)));

		return em.createQuery(cq).getResultList();
	}

	public List<District> getAllActiveByServerCountry() {
		CountryReferenceDto serverCountry = countryFacade.getServerCountry();

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<District> cq = cb.createQuery(getElementClass());
		Root<District> from = cq.from(getElementClass());
		Predicate filter = createBasicFilter(cb, from);

		if (serverCountry != null) {
			Path<Object> countryUuid = from.join(District.REGION, JoinType.LEFT).join(Region.COUNTRY, JoinType.LEFT)
					.get(Country.UUID);
			filter = CriteriaBuilderHelper.and(cb, filter,
					cb.or(cb.isNull(countryUuid), cb.equal(countryUuid, serverCountry.getUuid())));
		}

		cq.where(filter);
		cq.orderBy(cb.asc(from.get(District.NAME)));

		return em.createQuery(cq).getResultList();
	}

	public int getCountByRegion(Region region) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<District> from = cq.from(getElementClass());
		cq.select(cb.count(from));
		cq.where(cb.and(createBasicFilter(cb, from), cb.equal(from.get(District.REGION), region)));

		return em.createQuery(cq).getSingleResult().intValue();
	}

	public List<District> getByName(String name, Region region, boolean includeArchivedEntities) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<District> cq = cb.createQuery(getElementClass());
		Root<District> from = cq.from(getElementClass());

		Predicate filter = CriteriaBuilderHelper.unaccentedIlikePrecise(cb, from.get(District.NAME), name.trim());
		if (region != null) {
			filter = cb.and(filter, cb.equal(from.get(District.REGION), region));
		}
		if (!includeArchivedEntities) {
			filter = cb.and(filter, createBasicFilter(cb, from));
		}

		cq.where(filter);

		return em.createQuery(cq).getResultList();
	}

	public List<District> getByExternalId(Long ext_id, Region reg_ext_id, boolean includeArchivedEntities,
			int notUsed) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<District> cq = cb.createQuery(getElementClass());
		Root<District> from = cq.from(getElementClass());
		Predicate filter = cb.equal(from.get("externalId"), ext_id);

		// suspending region here temporarily
		if (reg_ext_id != null) {
			filter = cb.and(filter, cb.equal(from.get(District.REGION), reg_ext_id));
		}
		if (!includeArchivedEntities) {
			filter = cb.and(filter, createBasicFilter(cb, from));
		}

		cq.where(filter);
		return em.createQuery(cq).getResultList();
	}

	public List<District> getByExternalId(Long externalId, boolean includeArchivedEntities) {
		return getByExternalId(externalId, District.EXTERNAL_ID, includeArchivedEntities);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Predicate createUserFilter(CriteriaBuilder cb, CriteriaQuery cq, From<?, District> from) {
		// no filter by user needed
		return null;
	}

	public Predicate buildCriteriaFilter(DistrictCriteria criteria, CriteriaBuilder cb, Root<District> from) {
		Join<District, Region> region = from.join(District.REGION, JoinType.LEFT);
		Join<Region, Area> area = region.join(Region.AREA, JoinType.LEFT);

		Predicate filter = null;

		CountryReferenceDto country = criteria.getCountry();
		if (country != null) {
			CountryReferenceDto serverCountry = countryFacade.getServerCountry();

			Path<Object> countryUuid = from.join(District.REGION, JoinType.LEFT).join(Region.COUNTRY, JoinType.LEFT)
					.get(Country.UUID);
			Predicate countryFilter = cb.equal(countryUuid, country.getUuid());

			if (country.equals(serverCountry)) {
				filter = CriteriaBuilderHelper.and(cb, filter,
						CriteriaBuilderHelper.or(cb, countryFilter, countryUuid.isNull()));
			} else {
				filter = CriteriaBuilderHelper.and(cb, filter, countryFilter);
			}
		}

		if (criteria.getArea() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter, cb.equal(area.get(Area.UUID), criteria.getArea().getUuid()));
		}

		if (criteria.getRegion() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter, cb
					.equal(from.join(District.REGION, JoinType.LEFT).get(Region.UUID), criteria.getRegion().getUuid()));
		}

		if (criteria.getRisk() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter, cb.equal(from.get(District.RISK), criteria.getRisk()));
		}

		if (criteria.getNameEpidLike() != null) {
			String[] textFilters = criteria.getNameEpidLike().split("\\s+");
			for (String textFilter : textFilters) {
				if (DataHelper.isNullOrEmpty(textFilter)) {
					continue;
				}

				Predicate likeFilters = cb
						.or(CriteriaBuilderHelper.unaccentedIlike(cb, from.get(District.NAME), textFilter)// ,
						// CriteriaBuilderHelper.ilike(cb, from.get(District.EPID_CODE), textFilter)
						);
				filter = CriteriaBuilderHelper.and(cb, filter, likeFilters);
			}
		}

		if (criteria.getRelevanceStatus() != null) {

			System.out.println(criteria.getRelevanceStatus() + "relevamce status from service ");
			if (criteria.getRelevanceStatus() == EntityRelevanceStatus.ACTIVE) {
				filter = CriteriaBuilderHelper.and(cb, filter,
						cb.or(cb.equal(from.get(District.ARCHIVED), false), cb.isNull(from.get(District.ARCHIVED))));
			} else if (criteria.getRelevanceStatus() == EntityRelevanceStatus.ARCHIVED) {
				filter = CriteriaBuilderHelper.and(cb, filter, cb.equal(from.get(District.ARCHIVED), true));
			}else if (criteria.getRelevanceStatus() == EntityRelevanceStatus.ALL) {
				filter = CriteriaBuilderHelper.and(cb, filter);
			}
		}

		if (this.getCurrentUser().getArea() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter,
					cb.equal(area.get(Area.UUID), this.getCurrentUser().getArea().getUuid()));
		}
		return filter;
	}
	
	public Set<District> getByReferenceDto(Set<DistrictReferenceDto> district) {
		Set<District> districts = new HashSet<District>();
		for (DistrictReferenceDto com : district) {
			if (com != null && com.getUuid() != null) {
				District result = getByUuid(com.getUuid());
				if (result == null) {
					logger.warn("Could not find entity for " + com.getClass().getSimpleName() + " with uuid "
							+ com.getUuid());
				}
				districts.add(result);
			}
		}

		return districts;
	}
}
