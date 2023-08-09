import java.util.concurrent.RecursiveAction;

public class ParallelTest1 extends RecursiveAction {
    private BufferedImage img;
    private int x_start, x_stop;
    private int y_start, y_stop;
    private BufferedImage dstImg;
    protected static int sThreshold = 10000;

    public ParallelTest1(BufferedImage img, int x_strt, int x_stp, int y_strt, int y_stp,
            BufferedImage dst) {
        this.img = img;
        x_start = x_strt;
        x_stop = x_stp;
        y_start = y_strt;
        y_stop = y_stp;
        this.dstImg = dst;
    }

    protected void work() {
        int R, G, B, clr;
        for (int y = y_start; y < y_stop; y++) {
            for (int x = x_start; x < x_stop; x++) {
                clr = img.getRGB(x, y); // fix this
                R = (clr >> 16) & 0xff;
                G = (clr >> 8) & 0xff;
                B = (clr) & 0xff;
                R = Math.max(R, 200);
                G = Math.max(G, 200);
                B = Math.max(B, 200);
                clr = (0xff000000) | (R << 16) | (G << 8) | B; // convert to colour
                dstImg.setRGB(x, y, clr); // set pixel colout
            }
        }
    }

    protected void compute() {
        if ((x_stop - x_start) * (y_stop - y_start) < sThreshold) {
            work();
            return;
        }
        int splt = (x_stop - x_start) / 2;
        ParallelTest1 left = new ParallelTest1(img, x_start, x_start + splt, y_start, y_stop, dstImg);
        ParallelTest1 right = new ParallelTest1(img, x_start + splt, x_stop, y_start, y_stop, dstImg);
        left.fork();
        right.compute();
        left.join();
    }

    public static void main(String[] args) throws Exception {
        File f = new File("Images/image1.jpg");
        BufferedImage img = ImageIO.read(f);
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dstImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invoke(new ParallelTest1(img, 0, w, 0, h, dstImg));
        File dstFile = new File("Images/output.png");
        ImageIO.write(dstImg, "png", dstFile);
    }
}
