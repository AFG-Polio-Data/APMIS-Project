package de.symeda.sormas.app.backend.common;

import android.databinding.BaseObservable;
import android.util.Log;

import com.googlecode.openbeans.PropertyDescriptor;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Iterator;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import de.symeda.sormas.api.I18nProperties;

@MappedSuperclass
public abstract class AbstractDomainObject extends BaseObservable implements Serializable, Cloneable  {

	public static final String ID = "id";
	public static final String UUID = "uuid";
	public static final String SNAPSHOT = "snapshot";
	public static final String CREATION_DATE = "creationDate";
	public static final String CHANGE_DATE = "changeDate";
	public static final String LOCAL_CHANGE_DATE = "localChangeDate";
	public static final String MODIFIED = "modified";
	public static final String LAST_OPENED_DATE = "lastOpenedDate";

	@Id
	@GeneratedValue
	private Long id;

	/**
	 * This marks the snapshot of a modified entity that was created for merging
	 */
	@DatabaseField(uniqueCombo=true)
	private boolean snapshot = false;

	@DatabaseField(uniqueCombo=true, canBeNull = false)
	private String uuid;

	@DatabaseField(dataType = DataType.DATE_LONG, canBeNull = false)
	private Date creationDate;

	/**
	 * Date when the entity was last modified on the server
	 */
	@DatabaseField(dataType = DataType.DATE_LONG, canBeNull = false)
	private Date changeDate;

	/**
	 * Date when the entity was last updated from the server or locally modified.
	 */
	@DatabaseField(dataType = DataType.DATE_LONG, canBeNull = false, version = true)
	private Date localChangeDate;

	/**
	 * This entity or any embedded (not referenced) was modified and needs to be send to the server
	 */
	@DatabaseField
	private boolean modified = false;

	/**
	 * Date when the entity has been opened lastly.
	 */
	@DatabaseField(dataType = DataType.DATE_LONG)
	private Date lastOpenedDate;

	@Override
	public AbstractDomainObject clone() {
		try {
			return (AbstractDomainObject) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Used to figure out whether this entity or one of its embedded child entities has been modified, but
	 * not synchronized to the server yet. Needs to be overridden to check for child entities that don't
	 * have the @EmbeddedAdo annotation.
	 *
	 * @return
	 */
	public boolean isModifiedOrChildModified() {
		if (isModified()) return true;

		try {
			Iterator<PropertyDescriptor> propertyIterator = AdoPropertyHelper.getEmbeddedAdoProperties(this.getClass());
			while (propertyIterator.hasNext()) {
				PropertyDescriptor property = propertyIterator.next();
				AbstractDomainObject embeddedAdo = (AbstractDomainObject) property.getReadMethod().invoke(this);
				if (embeddedAdo == null) {
					throw new IllegalArgumentException("No embedded entity was created for " + property.getName());
				}

				if (embeddedAdo.isModifiedOrChildModified()) {
					return true;
				}
			}
		} catch (InvocationTargetException e) {
			Log.e(getClass().getName(), "Error while trying to invoke read method to request modified state", e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			Log.e(getClass().getName(), "Error while trying to invoke read method to request modified state", e);
			throw new RuntimeException(e);
		}

		return false;
	}

	public boolean isUnreadOrChildUnread() {
		if (lastOpenedDate == null) {
			return true;
		}

		if (lastOpenedDate.before(localChangeDate)) {
			return true;
		}

		// ignore this for now, so we can limit queries to the base data needed for list views #679
//		Iterator<PropertyDescriptor> propertyIterator = AdoPropertyHelper.getEmbeddedAdoProperties(this.getClass());
//		while (propertyIterator.hasNext()) {
//			try {
//				PropertyDescriptor property = propertyIterator.next();
//				AbstractDomainObject embeddedAdo = (AbstractDomainObject) property.getReadMethod().invoke(this);
//
//				if (embeddedAdo == null) {
//					throw new IllegalArgumentException("No embedded entity was created for " + property.getName());
//				}
//
//				if (embeddedAdo.isUnreadOrChildUnread()) {
//					return true;
//				}
//			} catch (InvocationTargetException e) {
//				Log.e(getClass().getName(), "Error while trying to invoke read method to request unread state", e);
//				throw new RuntimeException(e);
//			} catch (IllegalAccessException e) {
//				Log.e(getClass().getName(), "Error while trying to invoke read method to request unread state", e);
//				throw new RuntimeException(e);
//			}
//		}

		return false;
	}

	public String getI18nPrefix() {
		throw new UnsupportedOperationException("Can't retrieve I18N prefix of an unspecified ADO object");
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isSnapshot() {
		return snapshot;
	}

	public void setSnapshot(boolean snapshot) {
		this.snapshot = snapshot;
	}

	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getChangeDate() {
		return changeDate;
	}

	public void setChangeDate(Date changeDate) {
		this.changeDate = changeDate;
	}

	public void setChangeDateForNew() {
		this.changeDate = new Date(0);
	}

	public boolean isNew() {
		return changeDate == null || creationDate == null || changeDate.getTime() == 0;
	}

	public Date getLocalChangeDate() {
		return localChangeDate;
	}

	public void setLocalChangeDate(Date localChangeDate) {
		this.localChangeDate = localChangeDate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}

		if (o instanceof AbstractDomainObject) {
			// this works, because we are using UUIDs
			AbstractDomainObject ado = (AbstractDomainObject) o;
			return getUuid().equals(ado.getUuid());

		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getUuid().hashCode();
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public Date getLastOpenedDate() {
		return lastOpenedDate;
	}

	public void setLastOpenedDate(Date lastOpenedDate) {
		this.lastOpenedDate = lastOpenedDate;
	}

	@Override
	public String toString() {
		return I18nProperties.getFieldCaption(getI18nPrefix());
	}

}
