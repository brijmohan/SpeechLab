package edu.iiit.speech.facultycontact;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

/**
 * This class will manage the dialogs in the sense 1. Ensure completeness of
 * information 2. Ensures disambiguity in information 3. Maintain & record the
 * state of dialog
 * 
 * @author brij
 * 
 */
public class DialogManager {

	private FacultyDialogSlots dialogSlots;
	private FacultyContactActivity app;
	private FacultyDatabase db;
	private Util util;
	private static final String POPUP_TITLE = "";

	public DialogManager(Context context) {
		app = (FacultyContactActivity) context;
		db = new FacultyDatabase(context);
		util = new Util();
	}

	public boolean init(FacultyContextInfo contextInfo) {
		dialogSlots = new FacultyDialogSlots();
		boolean complete = manage(contextInfo);
		if (!complete) {
			app.phnumber_text
					.setText(app.getString(R.string.info_insufficient));
		}
		return complete;
	}

	public boolean manage(FacultyContextInfo contextInfo) {
		boolean info_complete = false;
		
		if (contextInfo.getName() != null && contextInfo.getName().equals("missed")) {
			dialogSlots.setInfoID("missed");
			info_complete = true;
		}
		else
		{
			FacultyVerifiedInfo verifyFacultyInfo = db
					.verifyFacultyInfo(contextInfo);
			if (dialogSlots.getFacultyID() == null) {
				if (verifyFacultyInfo.getFacultyID() == null
						|| verifyFacultyInfo.getFacultyID() == "") {

					appendDialogText(app.getString(R.string.name_not_found_text),
							"S");
				} else {
					// check for ambiguous info else assign to slot
					if (verifyFacultyInfo.getFacultyID().contains(",")) {
						StringBuilder sb = new StringBuilder();
						String[] split = verifyFacultyInfo.getFacultyID()
								.split(",");
						int count = 0;
						for (String fid : split) {
							sb.append(db.getFacultyNameById(fid));
							count++;
							if (count < split.length) {
								sb.append(", ");
							}
						}

						appendDialogText(
								app.getString(R.string.name_ambiguous_text) + " "
										+ contextInfo.getName() + " : "
										+ sb.toString(), "S");
					} else {
						dialogSlots.setFacultyID(verifyFacultyInfo.getFacultyID());
					}
				}
			} else {
				// Change faculty name with new one if uttered
				// TODO write logic to accept new name even if faculty name is set
			}
			if (dialogSlots.getInfoID() == null) {
				if (verifyFacultyInfo.getInfoTypeID() == null
						|| verifyFacultyInfo.getInfoTypeID() == "") {

					// Check if info id is not present
					// prompt to choose Info
					appendDialogText(app.getString(R.string.numtype_not_found), "S");

				} else {
					// check for ambiguous info else assign to slot
					dialogSlots.setInfoID(verifyFacultyInfo.getInfoTypeID());
				}
			}
			if (dialogSlots.getFacultyID() != null
					&& dialogSlots.getInfoID() != null) {
				info_complete = true;
			}
		}
		
		// return the value that signifies whether dialog has ended or not
		return info_complete;
	}

	private void appendDialogText(String text, String tag) {
		app.edit_text.setText(
				app.edit_text.getText()
						+ getFormattedText(text, tag).toString() + "\n");
		app.speakOut(text);
	}

	private Spanned getFormattedText(String text, String tag) {
		Spanned ftext = null;
		if ("S".equals(tag)) {
			ftext = Html.fromHtml("<font color='red'><b>" + tag + ": </b>" + text
					+ "</font>");
		} else {
			ftext = Html.fromHtml("<font color='green'><b>" + tag + ": </b>" + text
					+ "</font>");
		}
		System.out.println("text -------- > " + ftext);
		return ftext;
	}

	public void showResults() {
		if (dialogSlots.getInfoID().equals("missed")) {
			appendDialogText("There are currently no missed calls", "S");
		}
		else {
			appendDialogText(
					"Showing " + db.getInfoTypeNameByID(dialogSlots.getInfoID())
							+ " number of "
							+ db.getFacultyNameById(dialogSlots.getFacultyID()),
					"S");
			String facultyInfo = db.getFacultyInfo(dialogSlots.getFacultyID(),
					dialogSlots.getInfoID());
			app.phnumber_text.setText(facultyInfo);
			//app.listAdapter.add(facultyInfo);
			if (facultyInfo.matches("[0-9]+")) {
				app.call_button.setEnabled(true);
			}
		}
	}
}
