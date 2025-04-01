package com.example.examplemod.agent;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Screenshot {

    public static String captureScreenshot() {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();

        int width = window.getWidth();
        int height = window.getHeight();

        RenderTarget framebuffer = mc.getMainRenderTarget();
        framebuffer.bindRead();

        int size = width * height * 4;
        ByteBuffer buffer = BufferUtils.createByteBuffer(size);
        RenderSystem.readPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (x + (height - y - 1) * width) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        // Define the save location in the Documents folder
        String userHome = System.getProperty("user.home");
        File screenshotDir = new File(userHome, "Documents/MinecraftScreenshots");
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs(); // Create the directory if it does not exist
        }

        // Generate a unique filename with timestamp
        String filename = "screenshot_" + System.currentTimeMillis() + ".png";
        File file = new File(screenshotDir, filename);

        try {
            ImageIO.write(image, "PNG", file);
            System.out.println("Screenshot saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.toURI().toString();

    }
}

