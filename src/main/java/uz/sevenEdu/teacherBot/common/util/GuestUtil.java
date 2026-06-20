package uz.sevenEdu.teacherBot.common.util;

/**
 * Mehmon (testoviy o'quvchi) foydalanuvchi bilan ishlash uchun yordamchi.
 * Mehmon real akkaunt emas — progressi, ballari va statistikasi qayd etilmaydi.
 */
public final class GuestUtil {

    /** Seed qilingan yagona mehmon profilining ID'si (V14 migratsiya). */
    public static final Long GUEST_USER_ID = 999L;

    private GuestUtil() {}

    public static boolean isGuest(Long userId) {
        return userId != null && GUEST_USER_ID.equals(userId);
    }
}
