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
package de.symeda.sormas.ui;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.navigator.View;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import de.symeda.sormas.api.ConfigFacade;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.InfoProvider;

@SuppressWarnings("serial")
public class SupportView extends VerticalLayout implements View {

	public static final String VIEW_NAME = "support";

	public SupportView() {
		super();
		setMargin(true);
		
		CustomLayout content = new CustomLayout("support");
		content.setSizeFull();
		addComponent(content);
		
		SormasUI.getCurrent().getPage().getJavaScript().execute("        var form = document.getElementById('my-form');\n"
				+ "        form.addEventListener(\"submit\", e => {\n"
				+ "            e.preventDefault();\n"
				+ "            fetch(form.action, {\n"
				+ "                method: \"POST\",\n"
				+ "                body: new FormData(document.getElementById(\"my-form\")),\n"
				+ "            }).then((response) => {\n"
				+ "   \n"
				+ "    alert(\"You have submitted this form \");\n"
				+ "    window.location.reload();\n"
				+ "\n"
				+ "  \n"
				+ "});\n"
				+ "        });");
		
		Label infoLabel = new Label(
				I18nProperties.getCaption(Captions.aboutApmisVersion) + " " + InfoProvider.InfoProvider_apmis(),
				ContentMode.HTML);
		addComponent(infoLabel);
		
	}
	
}
