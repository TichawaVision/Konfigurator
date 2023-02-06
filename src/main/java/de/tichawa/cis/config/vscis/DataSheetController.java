package de.tichawa.cis.config.vscis;

/**
 * The datasheet controller for VSCIS. Needed subclass as a few portions do not follow all other CIS types.
 */
public class DataSheetController extends de.tichawa.cis.config.controller.DataSheetController {
    @Override
    protected String getProfileImageUrlString() {
        String key = CIS_DATA.getTiViKey();
        if (key.split("_")[4].endsWith("C") && getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile_coax.jpg") != null) {
            return "/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile_coax.jpg";
        } else
            return super.getProfileImageUrlString();
    }
}
