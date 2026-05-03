package org.w7ls.common.vk;

public class VkError extends RuntimeException {
    public VkError() {
        super("VKError");
    }
    public VkError(String m) {
        super("VKError: " + m);
    }
}
