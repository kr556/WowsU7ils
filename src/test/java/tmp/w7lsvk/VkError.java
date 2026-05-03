package tmp.w7lsvk;

public class VkError extends RuntimeException {
    public VkError() {
        super("VKError");
    }
    public VkError(String m) {
        super("VKError: " + m);
    }
}
