package com.cinoteck.application.views.campaigndata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.cinoteck.application.UserProvider;
import com.cinoteck.application.views.utils.importutils.DataImporter;
import com.cinoteck.application.views.utils.importutils.FileUploader;
import com.cinoteck.application.views.utils.importutils.PopulationDataImporter;
import com.opencsv.exceptions.CsvValidationException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.StreamResource;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ImportFacade;
import de.symeda.sormas.api.importexport.ValueSeparator;
import de.symeda.sormas.api.infrastructure.InfrastructureType;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.utils.DataHelper;

public class ImportCampaignsFormDataDialog extends Dialog {

//	ComboBox<CampaignReferenceDto> campaignFilter = new ComboBox<>();
	Label campaignFormMetaFilter = new Label();
	Button downloadImportTemplate = new Button(I18nProperties.getCaption(Captions.importDownloadImportTemplate));
	Button startDataImport = new Button(I18nProperties.getCaption(Captions.importImportData));
	public Button donloadErrorReport = new Button(I18nProperties.getCaption(Captions.importDownloadErrorReport));
//	ComboBox valueSeperator = new ComboBox<>();
	private boolean callbackRunning = false;
	private Timer timer;
//	private int pollCounter = 0;
	private File file_;
	Span anchorSpan = new Span();
	public Anchor downloadErrorReportButton;
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public ImportCampaignsFormDataDialog(CampaignReferenceDto campaignReferenceDto,
			CampaignFormMetaReferenceDto campaignForm) {
		String dto;
		if (campaignReferenceDto != null) {
			dto = campaignReferenceDto.getCaption();
		} else {
			dto = "New Campaign";
		}
		this.setHeaderTitle(I18nProperties.getString(Strings.headingImportCampaign) + " | " + dto + " ("
				+ campaignForm.getCaption() + ")");
//		this.getStyle().set("color" , "#0D6938");

		Hr seperatorr = new Hr();
		seperatorr.getStyle().set("color", " #0D6938");

		VerticalLayout dialog = new VerticalLayout();


//		campaignFilter.setId(CampaignDto.NAME);
//		campaignFilter.setRequired(true);
//		campaignFilter.setItems(FacadeProvider.getCampaignFacade().getAllActiveCampaignsAsReference());
//		campaignFilter.setValue(FacadeProvider.getCampaignFacade().getReferenceByUuid(campaignReferenceDto.getUuid()));
//		campaignFilter.setEnabled(false);

		campaignFormMetaFilter.setText(campaignForm.getCaption());

		Label lblCollectionDateInfo = new Label(I18nProperties.getCaption(Captions.actionImport));

		H3 step1 = new H3();
		step1.add("Step 1: Download the Import Template");
		Label lblImportTemplateInfo = new Label(I18nProperties.getString(Strings.infoDownloadCaseImportTemplate));

		Icon downloadImportTemplateButtonnIcon = new Icon(VaadinIcon.DOWNLOAD);
		downloadImportTemplate.setIcon(downloadImportTemplateButtonnIcon);
		downloadImportTemplate.addClickListener(e -> {

			try {

				String templateFilePath;
				String templateFileName;
				String fileNameAddition;
				ImportFacade importFacade = FacadeProvider.getImportFacade();

				importFacade.generateCampaignFormImportTemplateFile(campaignForm.getUuid());

				templateFileName = DataHelper.sanitizeFileName(campaignReferenceDto.getCaption().replaceAll(" ", "_"))
						+ "_" + DataHelper.sanitizeFileName(campaignForm.getCaption().replaceAll(" ", "_")) + ".csv";

				templateFilePath = importFacade.getCampaignFormImportTemplateFilePath();
				fileNameAddition = campaignForm.getCaption().replace(" ", "_") + "_campaignform_data_import_";

				String content = FacadeProvider.getImportFacade().getImportTemplateContent(templateFilePath);

				InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

				// Create a StreamResource
				StreamResource streamResource = new StreamResource(templateFileName, () -> inputStream);

				// Open the StreamResource in browser for download
				streamResource.setContentType("text/csv");
				streamResource.setCacheTime(0); // Disable caching

				// Create an anchor to trigger the download
				Anchor downloadAnchor = new Anchor(streamResource, "Download CSV");
				downloadAnchor.getElement().setAttribute("download", true);
				downloadAnchor.getStyle().set("display", "none");

				step1.add(downloadAnchor);

				// Simulate a click event on the hidden anchor to trigger the download
				downloadAnchor.getElement().callJsFunction("click");
				Notification.show("downloading...");

			} catch (IOException ioException) {
				ioException.printStackTrace();

				Notification.show(I18nProperties.getString(Strings.headingTemplateNotAvailable) + ": "
						+ I18nProperties.getString(Strings.messageTemplateNotAvailable));

			}

		}

		);

		H3 step2 = new H3();
		step2.add("Step 2: Import CSV File");
		Label lblImportCsvFile = new Label(I18nProperties.getString(Strings.infoImportCsvFile));
		Label sd = new Label("Upload");

//		MemoryBuffer memoryBuffer = new MemoryBuffer();
		FileUploader buffer = new FileUploader();
		Upload upload = new Upload(buffer);

		Icon startImportButtonnIcon = new Icon(VaadinIcon.UPLOAD);
		startDataImport.setIcon(startImportButtonnIcon);
		startDataImport.setVisible(false);
		upload.setAcceptedFileTypes("text/csv");

		upload.addSucceededListener(event -> {

			file_ = new File(buffer.getFilename());
			startDataImport.setVisible(true);

		});

		UserProvider usr = new UserProvider();
		UserDto userDto = usr.getUser();

		startDataImport.addClickListener(ed -> {

			try {

				// CampaignDto campaignDto =
				// FacadeProvider.getCampaignFacade().getByUuid(campaignFilter.getValue().getUuid());

				DataImporter importer = new CampaignFormDataImporter(file_, false, userDto, campaignForm.getUuid(),
						campaignReferenceDto, ValueSeparator.COMMA);
				importer.startImport(this::extendDownloadErrorReportButton, null, false, UI.getCurrent(), true);
			} catch (IOException | CsvValidationException e) {
				Notification.show(I18nProperties.getString(Strings.headingImportFailed) + " : "
						+ I18nProperties.getString(Strings.messageImportFailed));
			}

		});

		H3 step3 = new H3();
		step3.add("Step 3: Download Error Report");
		Label lblDnldErrorReport = new Label(I18nProperties.getString(Strings.infoDownloadErrorReport));
		downloadErrorReportButton = new Anchor("beforechange");
		// downloadErrorReportButton.setVisible(false);
		donloadErrorReport.setVisible(false);
		
		Icon downloadErrorReportButtonnIcon = new Icon(VaadinIcon.DOWNLOAD);
		donloadErrorReport.setIcon(downloadErrorReportButtonnIcon);
		donloadErrorReport.addClickListener(e -> {
			Notification.show("Button clicke to download error " + downloadErrorReportButton.getHref());

			downloadErrorReportButton.getElement().callJsFunction("click");
		});

		anchorSpan.add(downloadErrorReportButton);


		startIntervalCallback();
		

//		UI.getCurrent().addPollListener(event -> {
//			if (callbackRunning) {
//				System.out.println("starting pullers_________________");
//				UI.getCurrent().access(this::pokeFlow);
//			} else {
//				System.out.println("stoping pullers_________________");
//				stopPullers();
//			}
//		});

		dialog.add(seperatorr, 
				step1,
				lblImportTemplateInfo, downloadImportTemplate, step2, lblImportCsvFile, upload, startDataImport, step3,
				lblDnldErrorReport, donloadErrorReport, anchorSpan);

		Button doneButton = new Button("Done", e -> {
			close();
			stopIntervalCallback();

		});
		Icon doneButtonIcon = new Icon(VaadinIcon.CHECK_CIRCLE_O);
		doneButton.setIcon(doneButtonIcon);
		getFooter().add(doneButton);

		add(dialog);
		setCloseOnEsc(false);
		setCloseOnOutsideClick(false);

	}

	private void pokeFlow() {
		logger.debug("runingImport...");
	}

	private void startIntervalCallback() {
		//UI.getCurrent().setPollInterval(300);
		if (!callbackRunning) {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
//					stopIntervalCallback();
					pokeFlow();
				}
			}, 1000); // 10 minutes

			callbackRunning = true;
		}
	}

	private void stopIntervalCallback() {
		System.out.println("stopIntervalCallback_________________");
		if (callbackRunning) {
			callbackRunning = false;
			if (timer != null) {
				timer.cancel();
				timer.purge();
			}

		}
	}

//	private void stopPullers() {
//		UI.getCurrent().setPollInterval(-1);
//	}

	private void refreshPage() {
		// Get the current UI
		UI ui = UI.getCurrent();

		// Get the current page and reload it
		Page page = ui.getPage();
		page.reload();
	}

	protected void resetDownloadErrorReportButton() {
		downloadErrorReportButton.removeAll();
		downloadErrorReportButton.setVisible(false);
	}

	public void extendDownloadErrorReportButton(StreamResource streamResource) {
		anchorSpan.remove(downloadErrorReportButton);
		donloadErrorReport.setVisible(true);

		downloadErrorReportButton = new Anchor(streamResource, ".");// ,
																	// I18nProperties.getCaption(Captions.downloadErrorReport));
																	// I18nProperties.getCaption(Captions.importDownloadErrorReport)
		downloadErrorReportButton.setHref(streamResource);
		downloadErrorReportButton.setClassName("vaadin-button");

		anchorSpan.add(downloadErrorReportButton);

	}

}