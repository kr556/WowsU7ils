package org.w7ls.w7lsvk;

import org.junit.jupiter.api.Test;

import org.lwjgl.vulkan.VkExtensionProperties;
import tmp.w7lsvk.W7lsVkInstance;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

class W7lsVkInstanceTest {
    @Test
    void w7lsVkInstanceTest() {
        W7lsVkInstance vkInstance = new W7lsVkInstance(true);
        int[] p_extensionsCount = new int[1];

        vkEnumerateInstanceExtensionProperties((ByteBuffer) null, p_extensionsCount, null);
        var extensions = VkExtensionProperties.create(p_extensionsCount[0]);
        vkEnumerateInstanceExtensionProperties((ByteBuffer) null, p_extensionsCount, extensions);
        for (var extension : extensions)
            System.out.println(extension.extensionNameString());

    }
}