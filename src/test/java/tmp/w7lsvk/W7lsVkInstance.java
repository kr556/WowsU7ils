package tmp.w7lsvk;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public class W7lsVkInstance {
    final VkInstance handle;

    public W7lsVkInstance(boolean debug) {
        try (MemoryStack stack_ = stackPush()) {
            VkApplicationInfo appInfo_ = VkApplicationInfo.calloc(stack_);
            VkInstanceCreateInfo createInfo_ = VkInstanceCreateInfo.calloc(stack_);
            VkApplicationInfo appInfo = appInfo_
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack_.UTF8("w7lsVk"))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                    .apiVersion(VK_API_VERSION_1_3);

            PointerBuffer vallicationLayers = null;
            if (debug) {
                vallicationLayers = stack_.mallocPointer(1);
                vallicationLayers.put(stack_.ASCII("VK_LAYER_KHRONOS_validation")).flip();
            }

            VkInstanceCreateInfo createInfo = createInfo_
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledLayerNames(vallicationLayers)
                    .ppEnabledExtensionNames(glfwGetRequiredInstanceExtensions());

            PointerBuffer pInstance = stack_.mallocPointer(1);
            int stat;
            if ((stat = vkCreateInstance(createInfo, null, pInstance)) != VK_SUCCESS)
                throw new VkError("cant create vk instance. error " + stat);

            handle = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    public void destory() {
        vkDestroyInstance(handle, null);
    }
}