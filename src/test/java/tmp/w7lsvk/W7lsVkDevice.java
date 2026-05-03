package tmp.w7lsvk;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK13.*;

public class W7lsVkDevice {
    final VkPhysicalDevice physicalDevice;
    final VkDevice logicalDevice;
    final int graphicsFamily;
    final VkQueue graphicsQueue;

    public W7lsVkDevice(VkInstance instance, long surface) {
        try (MemoryStack stack_ = stackPush()) {

            IntBuffer count = stack_.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, count, null);

            PointerBuffer pDevices = stack_.mallocPointer(count.get(0));
            vkEnumeratePhysicalDevices(instance, count, pDevices);

            physicalDevice = new VkPhysicalDevice(pDevices.get(0), instance);

            graphicsFamily = findGraphicsFamily(stack_, surface);

            VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack_)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsFamily)
                    .pQueuePriorities(stack_.floats(1f));

            PointerBuffer extensions = stack_.mallocPointer(3);
            extensions.put(stack_.ASCII("VK_KHR_swapchain"));
            extensions.put(stack_.ASCII("VK_KHR_external_memory_win32"));
            extensions.put(stack_.ASCII("VK_KHR_external_memory"));
            extensions.flip();

            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack_)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfo)
                    .ppEnabledExtensionNames(extensions);

            PointerBuffer pDevice = stack_.mallocPointer(1);

            int stat;
            if ((stat = vkCreateDevice(physicalDevice, deviceCreateInfo, null, pDevice)) != VK_SUCCESS)
                throw new VkError("cant create device info. " + stat);
            logicalDevice = new VkDevice(pDevice.get(0), physicalDevice, deviceCreateInfo);

            PointerBuffer pQueue = stack_.mallocPointer(1);
            vkGetDeviceQueue(logicalDevice, graphicsFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), logicalDevice);
        }
    }

    public void destroy() {
        vkDestroyDevice(logicalDevice, null);
    }

    private int findGraphicsFamily(MemoryStack stack, long surface) {
        IntBuffer count = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null);
        VkQueueFamilyProperties.Buffer props =
                VkQueueFamilyProperties.malloc(count.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, props);

        IntBuffer pSupport = stack.mallocInt(1);
        for (int i = 0; i < props.capacity(); i++) {
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(
                    physicalDevice, i, surface, pSupport);
            if ((props.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0
                && pSupport.get(0) == VK_TRUE) {
                return i;
            }
        }
        throw new VkError("not found graphics queue.");
    }
}
