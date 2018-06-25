package de.symeda.sormas.app.caze;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.caze.CaseClassification;
import de.symeda.sormas.api.caze.CaseOutcome;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.caze.Case;
import de.symeda.sormas.app.backend.config.ConfigProvider;

/**
 * Created by Stefan Szczesny on 21.07.2016.
 */
public class CasesListArrayAdapter extends ArrayAdapter<Case> {

    private static final String TAG = CasesListArrayAdapter.class.getSimpleName();

    private final Context context;
    private int resource;
    private Case caze;
    private View convertView;

    public CasesListArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.context = context;
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(this.resource, parent, false);
        }

        this.convertView = convertView;

        caze = (Case)getItem(position);

        TextView uuid = (TextView) convertView.findViewById(R.id.cli_uuid);
        uuid.setText(DataHelper.getShortUuid(caze.getUuid()));

        TextView disease = (TextView) convertView.findViewById(R.id.cli_disease);
        disease.setText(caze.getDisease() != Disease.OTHER
                ? (caze.getDisease() != null ? caze.getDisease().toShortString() : "")
                : DataHelper.toStringNullable(caze.getDiseaseDetails()));

        TextView reportDate = (TextView) convertView.findViewById(R.id.cli_report_date);
        reportDate.setText(DateHelper.formatDate(caze.getReportDate()));

        TextView caseStatus = (TextView) convertView.findViewById(R.id.cli_case_satus);
        if (!(ConfigProvider.getUser().hasUserRole(UserRole.INFORMANT) && caze.getCaseClassification() == CaseClassification.NOT_CLASSIFIED)) {
            caseStatus.setText(caze.getCaseClassification() != null ? caze.getCaseClassification().toString() : "");
        } else {
            caseStatus.setText("");
        }

        TextView caseOutcome = (TextView) convertView.findViewById(R.id.cli_case_outcome);
        if (caze.getOutcome() == null || caze.getOutcome() == CaseOutcome.NO_OUTCOME) {
            caseOutcome.setVisibility(View.GONE);
        } else {
            caseOutcome.setVisibility(View.VISIBLE);
            caseOutcome.setText(caze.getOutcome().toString());
        }

        TextView person = (TextView) convertView.findViewById(R.id.cli_person);
        person.setText(caze.getPerson().toString());

        TextView facility = (TextView) convertView.findViewById(R.id.cli_facility);
        facility.setText(caze.getHealthFacility()!=null?caze.getHealthFacility().toString():null);

        TextView reporter = (TextView) convertView.findViewById(R.id.cli_reporter);
        reporter.setText(caze.getReportingUser()!=null?caze.getReportingUser().toString():null);

        ImageView synchronizedIcon = (ImageView) convertView.findViewById(R.id.cli_synchronized);
        if (caze.isModified()) {
            synchronizedIcon.setVisibility(View.VISIBLE);
            synchronizedIcon.setImageResource(R.drawable.ic_cached_black_18dp);
        } else {
            synchronizedIcon.setVisibility(View.GONE);
        }

        updateUnreadIndicator();

        return convertView;
    }

    public void updateUnreadIndicator() {
        if (caze != null && convertView != null) {
            LinearLayout itemLayout = (LinearLayout) convertView.findViewById(R.id.cases_list_item_layout);
            if (caze.isUnreadOrChildUnread()) {
                itemLayout.setBackgroundResource(R.color.bgColorUnread);
            } else {
                itemLayout.setBackground(null);
            }
        }
    }

}