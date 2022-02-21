package com.eyezah.cosmetics;

import com.eyezah.cosmetics.mixin.textures.MixinNativeImageAccessor;
import com.eyezah.cosmetics.utils.Debug;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.system.MemoryUtil;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class CosmeticaSkinManager {
//	@Override
//	public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
//		Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = this.original.getTextures(profile, requireSecure);
//
//		if (!textures.isEmpty()) { // if is a request that is returning a result
//			textures.put(MinecraftProfileTexture.Type.CAPE, new MinecraftProfileTexture(Cosmetica.apiServerHost + "/get/cloak?uuid=" + profile.getId() + "&username=" + profile.getName() + "&timestamp=" + System.currentTimeMillis() + "&token=" + Authentication.getToken(), new HashMap<>()));
//		}
//
//		return textures;
//		return this.original.getTextures(profile, requireSecure);
//	}

	public static URL getCosmeticaURL(URL fallback, final GameProfile profile, final boolean requireSecure) {
		String requestEndpoint = Cosmetica.apiServerHost + "/get/textures?";

		if (profile.isComplete()) {
			requestEndpoint += "uuid=" + profile.getId().toString() + "&username=" + profile.getName();
		} else if (profile.getId() == null) {
			requestEndpoint += "username=" + profile.getName();
		} else {
			requestEndpoint += "uuid=" + profile.getId();
		}

		try {
			requestEndpoint += "&token=" + Authentication.getToken() + "&timestamp=" + System.currentTimeMillis();
			return new URL(requestEndpoint);
		} catch (MalformedURLException e) {
			Cosmetica.LOGGER.error("Malformed URL on redirecting skin server to cosmetica???", e);
			return fallback;
		}
	}

	public static NativeImage processBadCapes(NativeImage image) {
		int srcWidth = image.getWidth();
		int srcHeight = image.getHeight();

		if (srcWidth != srcHeight * 2) {
			// very bad format from a very bad server
			int height = 32;
			int scaleFactor = 1;

			// get the right dimensions
			while (height < srcHeight) {
				height *= 2;
				scaleFactor *= 2;
			}

			// copy and dump dest
			NativeImage result = new NativeImage(height * 2, height, true);
			copyImage(image, result);

			// ancient cape format
			if (17 * srcWidth == 22 * srcHeight) {
				// time to make an elytra
				copyRect(result, 1, 1, 35, 4, 11, 16, scaleFactor);
				// on right
				clearRect(result, 44, 4, 2, 3, scaleFactor);
				clearRect(result, 45, 7, 1, 4, scaleFactor);
				// on left
				clearRect(result, 35, 16, 2, 4, scaleFactor);
				clearRect(result, 35, 11, 1, 5, scaleFactor);
				clearRect(result, 37, 19, 1, 1, scaleFactor);
				// expand below
				copyRect(result, 38, 19, 38, 20, 8, 1, scaleFactor);
				copyRect(result, 39, 19, 39, 21, 7, 1, scaleFactor);
				// expand above
				copyRect(result, 35, 4, 35, 3, 8, 1, scaleFactor);
				copyRect(result, 35, 4, 35, 2, 7, 1, scaleFactor);
				// the top bit thing
				copyRect(result, 12, 0, 31, 0 , 9, 1, scaleFactor);
				copyRect(result, 32, 0, 32, 1 , 2, 1, scaleFactor);
				copyRect(result, 35, 2, 34, 2, 2, 2, scaleFactor);
				// the edge bit
				copyRect(result, 45, 11, 22, 11 , 1, 11, scaleFactor);
			}

			// debug stuff
			String imgid = "cape_" + System.currentTimeMillis();
			Debug.dumpImages(imgid + "_src", true, image);
			Debug.dumpImages(imgid + "_formatted", true, result);

			image.close();
			return result;
		} else {
			return image;
		}
	}

	// fixed version of mojank code
	private static void copyImage(NativeImage src, NativeImage dest) {
		int width = Math.min(dest.getWidth(), src.getWidth());
		int height = Math.min(dest.getHeight(), src.getHeight());
		int bytesPerPixel = src.format().components();

		for(int l = 0; l < height; ++l) {
			int m = l * src.getWidth() * bytesPerPixel;
			int n = l * dest.getWidth() * bytesPerPixel;
			MemoryUtil.memCopy(
					((MixinNativeImageAccessor) (Object) src).getPixels() + (long)m,
					((MixinNativeImageAccessor) (Object) dest).getPixels() + (long)n,
					width * bytesPerPixel);
		}
	}

	private static void copyRect(NativeImage img, int srcX, int srcY, int destX, int destY, int width, int height, int scale) {
		srcX *= scale;
		srcY *= scale;

		destX *= scale;
		destY *= scale;

		width *= scale;
		height *= scale;

		for(int dx = 0; dx < width; ++dx) {
			for(int dy = 0; dy < height; ++dy) {
				int colour = img.getPixelRGBA(srcX + dx, srcY + dy);
				img.setPixelRGBA(destX + dx, destY + dy, colour);
			}
		}
	}

	private static void clearRect(NativeImage img, int x, int y, int width, int height, int scale) {
		x *= scale;
		y *= scale;

		width *= scale;
		height *= scale;

		for(int dx = 0; dx < width; ++dx) {
			for(int dy = 0; dy < height; ++dy) {
				img.setPixelRGBA(x + dx, y + dy, 0);
			}
		}
	}

	private record YggdrasilGameProfile(GameProfile gp, YggdrasilMinecraftSessionService ss) {
	}
}