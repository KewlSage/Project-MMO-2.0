package harmonised.pmmo.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotHandler
{
    public static final Logger LOGGER = LogManager.getLogger();

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    private static final File pmmoDir = new File( "screenshots/pmmo" );

    public static void takeScreenshot( String screenshotName, String folderName )
    {
        try
        {
            File screenshotDir = new File( pmmoDir, folderName );
            screenshotDir.mkdirs();
            Minecraft mc = Minecraft.getMinecraft();
            BufferedImage bufferedImage = ScreenShotHelper.createScreenshot(mc.getFramebuffer().framebufferWidth, mc.getFramebuffer().framebufferHeight, mc.getFramebuffer());
            String screenshotDate = DATE_FORMAT.format( new Date() );
            File screenshotFile = new File( screenshotDir, screenshotName + " " + screenshotDate + ".png" );

            ImageIO.write( bufferedImage, "png", screenshotFile );
        }
        catch ( Exception err )
        {
            LOGGER.info( "PMMO: FAILED TO TAKE SCREENSHOT", err );
        }
    }
}
