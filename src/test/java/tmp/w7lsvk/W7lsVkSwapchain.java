package tmp.w7lsvk;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;

public class W7lsVkSwapchain {
    public final long handle;
    final long[] images;
    final long[] imageViews;
    final int format;
    final int width, height;

    public W7lsVkSwapchain(W7lsVkDevice device, long surface, int w, int h) {
        try (MemoryStack stack = stackPush()) {

            // サーフェス能力を取得
            VkSurfaceCapabilitiesKHR caps =
                    VkSurfaceCapabilitiesKHR.malloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    device.physicalDevice, surface, caps);

            // フォーマット選択（BGRA8_SRGB優先）
            format = chooseSurfaceFormat(stack, device, surface);
            width  = w;
            height = h;

            int imageCount = Math.min(
                    caps.minImageCount() + 1,
                    caps.maxImageCount() > 0 ? caps.maxImageCount() : Integer.MAX_VALUE);

            VkSwapchainCreateInfoKHR swapInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface)
                    .minImageCount(imageCount)
                    .imageFormat(format)
                    .imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .imageExtent(e -> e.set(w, h))
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
                                | VK_IMAGE_USAGE_TRANSFER_DST_BIT) // CUDAコピー先に使う
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .preTransform(caps.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(VK_PRESENT_MODE_FIFO_KHR) // VSync
                    .clipped(true);

            LongBuffer pSwapchain = stack.mallocLong(1);
            int stat;
            if ((stat = vkCreateSwapchainKHR(device.logicalDevice, swapInfo, null, pSwapchain)) != VK_SUCCESS)
                throw new VkError("cant create swapcahin. " + stat);
            handle = pSwapchain.get(0);

            // イメージ取得
            IntBuffer imgCount = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device.logicalDevice, handle, imgCount, null);
            LongBuffer pImages = stack.mallocLong(imgCount.get(0));
            vkGetSwapchainImagesKHR(device.logicalDevice, handle, imgCount, pImages);

            images = new long[imgCount.get(0)];
            for (int i = 0; i < images.length; i++)
                images[i] = pImages.get(i);

            // ImageView作成
            imageViews = new long[images.length];
            for (int i = 0; i < images.length; i++)
                imageViews[i] = createImageView(stack, device, images[i], format);
        }
    }

    private int chooseSurfaceFormat(MemoryStack stack, W7lsVkDevice device, long surface) {
        IntBuffer count = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(device.physicalDevice, surface, count, null);
        VkSurfaceFormatKHR.Buffer formats =
                VkSurfaceFormatKHR.malloc(count.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(device.physicalDevice, surface, count, formats);

        for (VkSurfaceFormatKHR f : formats) {
            if (f.format() == VK_FORMAT_B8G8R8A8_SRGB)
                return f.format();
        }
        return formats.get(0).format();
    }

    private long createImageView(MemoryStack stack, W7lsVkDevice device,
                                 long image, int format) {
        VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(format)
                .subresourceRange(r -> r
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .levelCount(1)
                        .layerCount(1));

        LongBuffer pView = stack.mallocLong(1);
        int stat;
        if ((stat = vkCreateImageView(device.logicalDevice, viewInfo, null, pView)) != VK_SUCCESS)
            throw new VkError("cant crete image view. " + stat);
        return pView.get(0);
    }

    public void destroy(W7lsVkDevice device) {
        for (long view : imageViews)
            vkDestroyImageView(device.logicalDevice, view, null);
        vkDestroySwapchainKHR(device.logicalDevice, handle, null);
    }
}
