package uz.sevenEdu.teacherBot.lesson.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uz.sevenEdu.teacherBot.common.service.MediaProbeService.VideoInfo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VideoTranscodeServiceTest {

    @TempDir
    Path tmp;

    private static byte[] box(String type, int payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int size = 8 + payload;
        out.write(size >>> 24);
        out.write(size >>> 16);
        out.write(size >>> 8);
        out.write(size);
        out.writeBytes(type.getBytes(StandardCharsets.ISO_8859_1));
        out.writeBytes(new byte[payload]);
        return out.toByteArray();
    }

    private Path mp4(String... types) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String t : types) out.writeBytes(box(t, 32));
        Path p = Files.createTempFile(tmp, "v", ".mp4");
        Files.write(p, out.toByteArray());
        return p;
    }

    @Test
    void detectsFaststartByAtomOrder() throws Exception {
        assertTrue(VideoTranscodeService.isAlreadyFaststart(mp4("ftyp", "moov", "mdat")));
        assertFalse(VideoTranscodeService.isAlreadyFaststart(mp4("ftyp", "mdat", "moov")));
        assertFalse(VideoTranscodeService.isAlreadyFaststart(mp4("ftyp", "free")));
    }

    private static String arg(List<String> args, String flag) {
        return args.get(args.indexOf(flag) + 1);
    }

    @Test
    void hd30fpsGets480pAnd750k() {
        VideoInfo info = new VideoInfo(1920, 1080, 5_000_000L, 5_200_000L, 30.0, 600.0, true);
        List<String> args = VideoTranscodeService.transcodeArgs(Path.of("in.mp4"), Path.of("out.part"), info);
        assertEquals("750k", arg(args, "-maxrate"));
        assertEquals("1500k", arg(args, "-bufsize"));
        String vf = arg(args, "-vf");
        assertTrue(vf.contains("-2,480") && vf.contains("480,-2"), vf);
        assertFalse(vf.contains("fps="));
    }

    @Test
    void highFpsCappedAndLowBitrateSourceCapsMaxrate() {
        VideoInfo info = new VideoInfo(720, 1280, 600_000L, 700_000L, 59.94, 60.0, false);
        List<String> args = VideoTranscodeService.transcodeArgs(Path.of("in.mp4"), Path.of("out.part"), info);
        assertEquals("360k", arg(args, "-maxrate")); // 60% * 600k
        assertTrue(arg(args, "-vf").endsWith(",fps=30"));
    }

    @Test
    void smallSourceUses450kAndEvenTarget() {
        VideoInfo info = new VideoInfo(641, 359, null, null, 25.0, null, true);
        List<String> args = VideoTranscodeService.transcodeArgs(Path.of("in.mp4"), Path.of("out.part"), info);
        assertEquals("450k", arg(args, "-maxrate"));
        assertTrue(arg(args, "-vf").contains("-2,358"));
    }
}
