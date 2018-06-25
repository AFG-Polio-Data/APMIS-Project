package de.symeda.sormas.ui.statistics;

import java.util.List;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;

import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Grid;

import de.symeda.sormas.api.I18nProperties;
import de.symeda.sormas.api.statistics.StatisticsCaseAttribute;
import de.symeda.sormas.api.statistics.StatisticsCaseCriteria;
import de.symeda.sormas.api.statistics.StatisticsCaseSubAttribute;
import de.symeda.sormas.api.statistics.StatisticsGroupingKey;
import de.symeda.sormas.api.statistics.StatisticsHelper;
import de.symeda.sormas.api.statistics.StatisticsHelper.StatisticsKeyComparator;
import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class StatisticsCaseGrid extends Grid {

	private static final String ROW_CAPTION_COLUMN = "RowCaptionColumn";
	private static final String CASE_COUNT_COLUMN = "CaseCountColumn";
	private static final String UNKNOWN_COLUMN = "UnknownColumn";
	private static final String TOTAL_COLUMN = "TotalColumn";

	private final class StatisticsCaseGridCellStyleGenerator implements CellStyleGenerator {
		private static final long serialVersionUID = 1L;

		@Override
		public String getStyle(CellReference cell) {
			if (cell.getPropertyId().equals(ROW_CAPTION_COLUMN)) {
				return CssStyles.GRID_ROW_TITLE;
			}

			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public StatisticsCaseGrid(StatisticsCaseAttribute rowsAttribute, StatisticsCaseSubAttribute rowsSubAttribute,
			StatisticsCaseAttribute columnsAttribute, StatisticsCaseSubAttribute columnsSubAttribute, boolean zeroValues, 
			List<Object[]> content, StatisticsCaseCriteria caseCriteria) {

		super();

		setSelectionMode(SelectionMode.NONE);
		setHeightMode(HeightMode.UNDEFINED);
		setWidth(100, Unit.PERCENTAGE);
		setCellStyleGenerator(new StatisticsCaseGridCellStyleGenerator());

		if (content.isEmpty()) {
			return;
		}

		// If no displayed attributes are selected, simply show the total number of cases
		if (rowsAttribute == null && columnsAttribute == null) {
			addColumn(CASE_COUNT_COLUMN);
			getColumn(CASE_COUNT_COLUMN).setHeaderCaption(I18nProperties.getFragment(StatisticsHelper.CASE_COUNT));
			addRow(new Object[]{String.valueOf(content.get(0)[0])});
			return;
		}

		// Extract columns from content and add them to the grid
		Column captionColumn = addColumn(ROW_CAPTION_COLUMN);
		captionColumn.setHeaderCaption("");
		captionColumn.setSortable(false);
		captionColumn.setMaximumWidth(150);

		TreeMap<StatisticsGroupingKey, String> columns = new TreeMap<>(new StatisticsKeyComparator());
		if (columnsAttribute == null && columnsSubAttribute == null) {
			// When no column grouping has been selected, simply display the number of cases for the respective row
			addColumn(CASE_COUNT_COLUMN);
			getColumn(CASE_COUNT_COLUMN).setHeaderCaption(I18nProperties.getFragment(StatisticsHelper.CASE_COUNT));
		} else {
			boolean addColumnUnknown = false;
			// Iterate over content and add new columns to the list
			for (Object[] entry : content) {
				if (StatisticsHelper.isNullOrUnknown(entry[entry.length - 1])) {
					addColumnUnknown = true;
				} else {
					columns.putIfAbsent((StatisticsGroupingKey) entry[entry.length - 1], entry[entry.length - 1].toString());
				}
			}

			// If zero values are ticked, add missing columns to the list; this involves every possible value of the chosen column attribute unless a filter has been
			// set for the same attribute; in this case, only values that are part of the filter are chosen
			if (zeroValues) {
				List<Object> values = StatisticsHelper.getAllAttributeValues(columnsAttribute, columnsSubAttribute);
				List<StatisticsGroupingKey> filterValues = (List<StatisticsGroupingKey>) caseCriteria.getFilterValuesForGrouping(columnsAttribute, columnsSubAttribute);
				for (Object value : values) {
					Object formattedValue = StatisticsHelper.buildGroupingKey(value, columnsAttribute, columnsSubAttribute);
					if (formattedValue != null && (CollectionUtils.isEmpty(filterValues) || filterValues.contains(formattedValue))) {
						columns.putIfAbsent((StatisticsGroupingKey) formattedValue, formattedValue.toString()); 
					}
				}
			}

			// Add all collected columns to the grid
			for (StatisticsGroupingKey columnId : columns.keySet()) {
				Column column = addColumn(columnId);
				column.setHeaderCaption(columns.get(columnId));
				column.setSortable(false);
				column.setMaximumWidth(120);
			}

			// Add the column to display unknown numbers if required
			if (addColumnUnknown) {
				Column column = addColumn(UNKNOWN_COLUMN);
				column.setHeaderCaption(I18nProperties.getFragment(StatisticsHelper.UNKNOWN));
				column.setSortable(false);
				column.setMaximumWidth(120);
			}

			// Add total column
			Column totalColumn = addColumn(TOTAL_COLUMN);
			totalColumn.setHeaderCaption(I18nProperties.getFragment(StatisticsHelper.TOTAL));
			totalColumn.setSortable(false);
		}

		// Extract rows from content and add them to the grid
		if (rowsAttribute == null && rowsSubAttribute == null) {
			// When no row grouping has been selected, simply display the number of cases for the respective column
			Object[] row = new Object[getColumns().size()];
			row[0] = I18nProperties.getFragment(StatisticsHelper.CASE_COUNT);
			long totalAmountOfCases = 0;
			for (int i = 0; i < content.size(); i++) {
				Object[] entry = content.get(i);
				if (StatisticsHelper.isNullOrUnknown(entry[1])) {
					// Unknown column is always the second-last
					int columnIndex = getColumns().size() - 2;
					row[columnIndex] = entry[0].toString();
				} else {
					// Retrieve the column index by using the headMap function
					int columnIndex = columns.headMap((StatisticsGroupingKey) entry[entry.length - 1]).size() + 1;
					row[columnIndex] = entry[0].toString();
				}
				totalAmountOfCases += (long) entry[0];
			}
			row[row.length - 1] = String.valueOf(totalAmountOfCases);
			addRow(row);
		} else {
			TreeMap<StatisticsGroupingKey, Object[]> rows = new TreeMap<>(new StatisticsKeyComparator());
			Object[] currentRow = null;
			Object[] unknownRow = null;
			StatisticsGroupingKey currentRowKey = null;
			long[] columnTotals = new long[getColumns().size()];

			for (Object[] entry : content) {
				if (columnsAttribute == null && columnsSubAttribute == null) {
					// Only display the number of cases if there is no column grouping
					if (!StatisticsHelper.isNullOrUnknown(entry[entry.length - 1])) {
						currentRow = new Object[getColumns().size()];
						currentRowKey = (StatisticsGroupingKey) entry[entry.length - 1];
						currentRow[0] = entry[entry.length - 1].toString();
						currentRow[1] = entry[0].toString();
						rows.putIfAbsent((StatisticsGroupingKey) entry[entry.length - 1], currentRow);
						columnTotals[columnTotals.length - 1] += (long) entry[0];
					} else {
						unknownRow = new Object[getColumns().size()];
						unknownRow[0] = I18nProperties.getFragment(StatisticsHelper.UNKNOWN);
						unknownRow[1] = entry[0].toString();
						columnTotals[columnTotals.length - 1] += (long) entry[0];
					}
				} else {
					// New grouping entry has been reached, add the current row to the rows map
					if (currentRow != null && currentRowKey != null && !currentRowKey.equals(entry[1])) {
						int totalForRow = calculateTotalForRow(currentRow);
						currentRow[currentRow.length - 1] = String.valueOf(totalForRow);
						columnTotals[columnTotals.length - 1] += totalForRow;
						rows.putIfAbsent(currentRowKey, currentRow);
						currentRow = null;
						currentRowKey = null;
					}

					// New grouping entry has been reached, set up currentRow and currentRowKey
					if (currentRow == null && currentRowKey == null) {
						if (StatisticsHelper.isNullOrUnknown(entry[1])) {
							if (unknownRow == null) {
								unknownRow = new Object[getColumns().size()];
								unknownRow[0] = I18nProperties.getFragment(StatisticsHelper.UNKNOWN);
							}
						} else {
							currentRow = new Object[getColumns().size()];
							currentRowKey = (StatisticsGroupingKey) entry[1];
							currentRow[0] = entry[1].toString();
						}
					}

					// Add value to the row on the respective column index
					int columnIndex = 0;
					
					if (!StatisticsHelper.isNullOrUnknown(entry[entry.length - 1])) {
						columnIndex = columns.headMap((StatisticsGroupingKey) entry[entry.length - 1]).size() + 1;
					} else {
						columnIndex = getColumns().size() - 2;
					}
					
					if (StatisticsHelper.isNullOrUnknown(entry[1])) {
						unknownRow[columnIndex] = entry[0].toString();
					} else {
						currentRow[columnIndex] = entry[0].toString();
					}
					
					columnTotals[columnIndex] += (long) entry[0];
				}
			}

			// Add the last calculated row to the list (since this was not done in the loop) or calculate
			// the totals for the unknown row if this was the last row processed
			if (columnsAttribute != null || columnsSubAttribute != null) {
				if (currentRow != null && currentRow[0] != null) {
					int totalForRow = calculateTotalForRow(currentRow);
					currentRow[currentRow.length - 1] = String.valueOf(totalForRow);
					columnTotals[columnTotals.length - 1] += totalForRow;
					rows.putIfAbsent(currentRowKey, currentRow);
				} else if (unknownRow != null && unknownRow[0] != null) {
					int totalForRow = calculateTotalForRow(unknownRow);
					unknownRow[unknownRow.length - 1] = String.valueOf(totalForRow);
					columnTotals[columnTotals.length - 1] += totalForRow;
				}
			}

			// If zero values are ticked, add missing rows to the list; this involves every possible value of the chosen row attribute unless a filter has been
			// set for the same attribute; in this case, only values that are part of the filter are chosen
			if (zeroValues) {
				List<Object> values = StatisticsHelper.getAllAttributeValues(rowsAttribute, rowsSubAttribute);
				List<StatisticsGroupingKey> filterValues = (List<StatisticsGroupingKey>) caseCriteria.getFilterValuesForGrouping(rowsAttribute, rowsSubAttribute);
				for (Object value : values) {
					Object formattedValue = StatisticsHelper.buildGroupingKey(value, rowsAttribute, rowsSubAttribute);
					if (formattedValue != null && (CollectionUtils.isEmpty(filterValues) || filterValues.contains(formattedValue))) {
						Object[] zeroRow = new Object[getColumns().size()];
						zeroRow[0] = formattedValue.toString();
						zeroRow[zeroRow.length - 1] = null;
						rows.putIfAbsent((StatisticsGroupingKey) formattedValue, zeroRow); 
					}
				}
			}

			// Add rows to the grid
			for (StatisticsGroupingKey groupingKey : rows.keySet()) {
				addRow(rows.get(groupingKey));
			}

			// Add unknown row if existing
			if (unknownRow != null) {
				addRow(unknownRow);
			}

			// Add total row
			Object[] totalRow = new Object[getColumns().size()];
			totalRow[0] = I18nProperties.getFragment(StatisticsHelper.TOTAL);
			for (int i = 1; i < columnTotals.length; i++) {
				if (columnTotals[i] > 0) {
					totalRow[i] = String.valueOf(columnTotals[i]);
				} else {
					totalRow[i] = null;
				}
			}
			addRow(totalRow);
		}
	}

	private int calculateTotalForRow(Object[] row) {
		int total = 0;
		for (int i = 1; i < row.length; i++) {
			if (row[i] != null) {
				total += Integer.valueOf(row[i].toString());
			}
		}

		return total;
	}

}
