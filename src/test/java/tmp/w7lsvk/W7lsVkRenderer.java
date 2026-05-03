package tmp.w7lsvk;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.w7ls.common.cuda.CUDAKernel;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;

public class W7lsVkRenderer {

    private final W7lsVkDevice device;
    private final W7lsVkSwapchain swapchain;
    private W7lsVkExternalBuffer externalBuffer;
    private List<CUDAKernel> cudaKernels = new ArrayList<>();

    private final long commandPool;
    private final VkCommandBuffer[] commandBuffers;

    private final long[] imageAvailableSemaphores;
    private final long[] renderFinishedSemaphores;
    private final long[] inFlightFences;

    private static final int MAX_FRAMES_IN_FLIGHT = 2;
    private int currentFrame = 0;

    public W7lsVkRenderer(W7lsVkDevice device, W7lsVkSwapchain swapchain) {
        this.device   = device;
        this.swapchain = swapchain;

        commandPool              = createCommandPool();
        commandBuffers           = allocateCommandBuffers();
        imageAvailableSemaphores = createSemaphores();
        renderFinishedSemaphores = createSemaphores();
        inFlightFences           = createFences();
    }

    // ── コマンドプール ────────────────────────────────
    private long createCommandPool() {
        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(device.graphicsFamily);

            LongBuffer pPool = stack.mallocLong(1);
            int stat;
            if ((stat = vkCreateCommandPool(device.logicalDevice, info, null, pPool)) != VK_SUCCESS)
                throw new VkError("cant create command pool. " + stat);
            return pPool.get(0);
        }
    }

    // ── コマンドバッファ ──────────────────────────────
    private VkCommandBuffer[] allocateCommandBuffers() {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo info = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(MAX_FRAMES_IN_FLIGHT);

            PointerBuffer pBuffers = stack.mallocPointer(MAX_FRAMES_IN_FLIGHT);
            int stat;
            if ((stat = vkAllocateCommandBuffers(device.logicalDevice, info, pBuffers)) != VK_SUCCESS)
                throw new VkError("cant allocate command buffers. " + stat);

            // PointerBuffer → VkCommandBuffer[] に変換
            VkCommandBuffer[] result = new VkCommandBuffer[MAX_FRAMES_IN_FLIGHT];
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++)
                result[i] = new VkCommandBuffer(pBuffers.get(i), device.logicalDevice);
            return result;
        }
    }

    // ── セマフォ ──────────────────────────────────────
    private long[] createSemaphores() {
        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo info = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            long[] result = new long[MAX_FRAMES_IN_FLIGHT];
            LongBuffer pSem = stack.mallocLong(1);
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                int stat;
                if ((stat = vkCreateSemaphore(device.logicalDevice, info, null, pSem)) != VK_SUCCESS)
                    throw new VkError("cant create semaphore. " + stat);
                result[i] = pSem.get(0);
            }
            return result;
        }
    }

    // ── フェンス ──────────────────────────────────────
    private long[] createFences() {
        try (MemoryStack stack = stackPush()) {
            VkFenceCreateInfo info = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT); // 最初からSignaled

            long[] result = new long[MAX_FRAMES_IN_FLIGHT];
            LongBuffer pFence = stack.mallocLong(1);
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                int stat;
                if ((stat = vkCreateFence(device.logicalDevice, info, null, pFence)) != VK_SUCCESS)
                    throw new VkError("cant create fence. " + stat);
                result[i] = pFence.get(0);
            }
            return result;
        }
    }

    // ── 毎フレーム呼ぶ ────────────────────────────────
    public void drawFrame() {
        try (MemoryStack stack = stackPush()) {

            // 前のフレームの完了を待つ
            vkWaitForFences(device.logicalDevice,
                    stack.longs(inFlightFences[currentFrame]), true, Long.MAX_VALUE);
            vkResetFences(device.logicalDevice,
                    stack.longs(inFlightFences[currentFrame]));

            // 次のスワップチェーンイメージを取得
            IntBuffer pImageIndex = stack.mallocInt(1);
            int stat = vkAcquireNextImageKHR(
                    device.logicalDevice,
                    swapchain.handle,
                    Long.MAX_VALUE,
                    imageAvailableSemaphores[currentFrame],
                    VK_NULL_HANDLE,
                    pImageIndex);

            if (stat == VK_ERROR_OUT_OF_DATE_KHR) {
                // ウィンドウリサイズ時などはここで再作成が必要
                return;
            }
            int imageIndex = pImageIndex.get(0);

            // コマンドバッファ記録
            recordCommandBuffer(imageIndex);

            // キューにサブミット
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(stack.longs(imageAvailableSemaphores[currentFrame]))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(commandBuffers[currentFrame]))
                    .pSignalSemaphores(stack.longs(renderFinishedSemaphores[currentFrame]));

            if ((stat = vkQueueSubmit(device.graphicsQueue, submitInfo,
                    inFlightFences[currentFrame])) != VK_SUCCESS)
                throw new VkError("cant submit queue. " + stat);

            // 画面に表示
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(stack.longs(renderFinishedSemaphores[currentFrame]))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(swapchain.handle))
                    .pImageIndices(pImageIndex);

            vkQueuePresentKHR(device.graphicsQueue, presentInfo);

            currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    // ── コマンドバッファ記録 ──────────────────────────
    private void recordCommandBuffer(int imageIndex) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer cmd = commandBuffers[currentFrame];
            vkResetCommandBuffer(cmd, 0);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            int stat;
            if ((stat = vkBeginCommandBuffer(cmd, beginInfo)) != VK_SUCCESS)
                throw new VkError("cant begin command buffer. " + stat);

            // イメージレイアウトを転送先に変換
            transitionImageLayout(stack, cmd,
                    swapchain.images[imageIndex],
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            // 画面をクリア（黒）
            VkClearColorValue clearColor = VkClearColorValue.calloc(stack);
            clearColor.float32(0, 0.0f); // R
            clearColor.float32(1, 0.0f); // G
            clearColor.float32(2, 0.0f); // B
            clearColor.float32(3, 1.0f); // A

            VkImageSubresourceRange range = VkImageSubresourceRange.calloc(stack)
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .levelCount(1)
                    .layerCount(1);

            vkCmdClearColorImage(cmd, swapchain.images[imageIndex], VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, clearColor, range);

            // 表示用レイアウトに変換
            transitionImageLayout(stack, cmd,
                    swapchain.images[imageIndex],
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            if ((stat = vkEndCommandBuffer(cmd)) != VK_SUCCESS)
                throw new VkError("cant end command buffer. " + stat);
        }
    }

    // ── イメージレイアウト変換 ────────────────────────
    private void transitionImageLayout(MemoryStack stack, VkCommandBuffer cmd,
                                       long image, int oldLayout, int newLayout) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image)
                .subresourceRange(r -> r
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .levelCount(1)
                        .layerCount(1));

        int srcStage, dstStage;
        if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED) {
            barrier.get(0)
                    .srcAccessMask(0)
                    .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
        } else {
            barrier.get(0)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(0);
            srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            dstStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
        }

        vkCmdPipelineBarrier(cmd,
                srcStage, dstStage, 0,
                null, null, barrier);
    }

    public void destroy() {
        vkDeviceWaitIdle(device.logicalDevice);

        try (MemoryStack stack = stackPush()) {
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                vkDestroySemaphore(device.logicalDevice, imageAvailableSemaphores[i], null);
                vkDestroySemaphore(device.logicalDevice, renderFinishedSemaphores[i], null);
                vkDestroyFence(device.logicalDevice, inFlightFences[i], null);
            }
        }
        vkDestroyCommandPool(device.logicalDevice, commandPool, null);
    }
}