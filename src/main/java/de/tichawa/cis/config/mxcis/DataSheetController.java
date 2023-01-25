package de.tichawa.cis.config.mxcis;

/**
 * The datasheet controller for MXCIS. Needed subclass as a few portions do not follow all other CIS types.
 */
public class DataSheetController extends de.tichawa.cis.config.DataSheetController {

    @Override
    protected String getProfileImageUrlString() {
        String key = CIS_DATA.getTiViKey();
        String append = "";

        if (key.split("_")[5].endsWith("C")) {
            append += "_coax";
        }

        if (key.split("_")[5].startsWith("2")) {
            append += "_L2";
        } else {
            append += "_L1";
        }
        return "/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile" + append + ".jpg";
    }
}
