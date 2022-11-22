package de.symeda.sormas.api.user;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public enum FormAccess {
	ICM, PCA, REPORT, FLW, ADMIN, FMS, LQAS ; //.PRE

	public void addAssignableForms(Collection<FormAccess> collection) {

		for (FormAccess form : FormAccess.values()) {
			collection.add(form);
		}
	}

	public static Set<FormAccess> getAssignableForms() {
		Set<FormAccess> result = EnumSet.allOf(FormAccess.class);

		return result;
	}

}
