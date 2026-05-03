package org.w7ls.common.vk;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import org.lwjgl.vulkan.VkExtensionProperties;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VkDeviceCreateInfo.*;
import static org.lwjgl.vulkan.VkDeviceCreateInfo.*;
import static org.lwjgl.vulkan.VkExtensionProperties.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;

public class W7lsVkInstance {
    VkInstance instance;
    VkExtensionProperties.Buffer extensionProperties;

    public static void main(String[] args) {
        new W7lsVkInstance("", (a, b, c, d) -> {
            System.out.println(3);
            return 3;
        });
    }

    public W7lsVkInstance(String appName, VkDebugUtilsMessengerCallbackEXTI debugCallback) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8(appName))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                    .apiVersion(VK_API_VERSION_1_3);

            PointerBuffer vallicationLayers = null;
            if (debugCallback != null) {
                vallicationLayers = stack.mallocPointer(1);
                vallicationLayers.put(stack.ASCII("VK_LAYER_KHRONOS_validation")).flip();
            }

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledLayerNames(vallicationLayers)
                    .ppEnabledExtensionNames(glfwGetRequiredInstanceExtensions());

            PointerBuffer pInstance = stack.mallocPointer(1);
            int res;
            if ((res = vkCreateInstance(instanceCreateInfo, null, pInstance)) != VK_SUCCESS)
                throw new VkError("failed create vk instance. : " + res);

            instance = new VkInstance(pInstance.get(0), instanceCreateInfo);

            int[] eCount = new int[1];
            vkEnumerateInstanceExtensionProperties((ByteBuffer) null, eCount, null);
            var extensions = VkExtensionProperties.calloc(eCount[0]);
            vkEnumerateInstanceExtensionProperties((ByteBuffer) null, eCount, extensions);

            extensionProperties = extensions;

            if (debugCallback != null) {
                VkDebugUtilsMessengerCreateInfoEXT createInfoEXT = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                        .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                                         VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                         VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                        .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                     VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                     VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                        .pfnUserCallback(debugCallback)
                        .pUserData(VK_NULL_HANDLE);
            }
        }
    }


    public void destroy() {
        vkDestroyInstance(instance, null);
    }
}
