package tmp.w7lsvk;

import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class W7lsVk {
    private W7lsVkInstance instance;
    private W7lsVkDevice device;
    private W7lsVkSwapchain swapchain;
    private W7lsVkRenderer renderer;
    private final int width;
    private final int height;
    private final long window;
    private final long surface;

    public W7lsVk(int width, int height, String windowName) {
        try (MemoryStack stack = stackPush()) {
            this.width = width;
            this.height = height;

            glfwInit();
            glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
            window = glfwCreateWindow(width, height, windowName, 0, 0);

            instance = new W7lsVkInstance(false);

            LongBuffer pSurface = stack.mallocLong(1);
            int stat;
            if ((stat = glfwCreateWindowSurface(instance.handle, window, null, pSurface)) != VK_SUCCESS)
                throw new VkError("cant create window. " + stat);
            surface = pSurface.get(0);
            device = new W7lsVkDevice(instance.handle, surface);
            swapchain = new W7lsVkSwapchain(device, surface, width, height);
            renderer = new W7lsVkRenderer(device, swapchain);
        }
    }

    public void run() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            renderer.drawFrame();
        }

        renderer.destroy();
        swapchain.destroy(device);
        device.destroy();
        vkDestroySurfaceKHR(instance.handle, surface, null);
        instance.destory();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
