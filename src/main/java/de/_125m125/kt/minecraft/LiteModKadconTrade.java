package de._125m125.kt.minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.lwjgl.input.Keyboard;

import com.google.common.eventbus.EventBus;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mumfrey.liteloader.JoinGameListener;
import com.mumfrey.liteloader.ShutdownListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.modconfig.ConfigStrategy;
import com.mumfrey.liteloader.modconfig.ExposableOptions;

import de._125m125.kt.ktapi.core.KtCachingRequester;
import de._125m125.kt.ktapi.core.KtNotificationManager;
import de._125m125.kt.ktapi.core.SingleUserKtRequester;
import de._125m125.kt.ktapi.core.entities.Permissions;
import de._125m125.kt.ktapi.core.results.Callback;
import de._125m125.kt.ktapi.core.users.CertificateUser;
import de._125m125.kt.ktapi.core.users.KtUserStore;
import de._125m125.kt.ktapi.core.users.TokenUser;
import de._125m125.kt.ktapi.core.users.UserKey;
import de._125m125.kt.ktapi.okhttp.websocket.KtOkHttpWebsocket;
import de._125m125.kt.ktapi.retrofit.KtRetrofit;
import de._125m125.kt.ktapi.retrofit.requester.KtRetrofitRequester;
import de._125m125.kt.ktapi.smartcache.KtSmartCache;
import de._125m125.kt.ktapi.websocket.KtWebsocketManager;
import de._125m125.kt.ktapi.websocket.events.listeners.AutoReconnectionHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.KtWebsocketNotificationHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.OfflineMessageHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.SessionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.util.text.TextComponentString;

/**
 * This is a very simple example LiteMod, it draws an analogue clock on the
 * minecraft HUD using a traditional onTick hook supplied by LiteLoader's
 * {@link Tickable} interface.
 *
 * @author Adam Mummery-Smith
 */
@ExposableOptions(strategy = ConfigStrategy.Versioned, filename = "kt.json")
public class LiteModKadconTrade implements Tickable, JoinGameListener, ShutdownListener {
	/**
	 * This is a keybinding that we will register with the game and use to toggle
	 * the clock
	 * 
	 * Notice that we specify the key name as an *unlocalised* string. The
	 * localisation is provided from the included resource file.
	 */
	private static KeyBinding ktKeyBinding = new KeyBinding("key.clock.toggle", Keyboard.KEY_F12,
			"key.categories.litemods");
	@Expose
	@SerializedName("uid")
	private String uid = "";
	@Expose
	@SerializedName("tid")
	private String tid = "";
	@Expose
	@SerializedName("tkn")
	private EncryptedData encrTkn = null;

	private String tkn;
	private LoginState state;

	private KtUserStore userStore = new KtUserStore();
	private KtRetrofitRequester requester;
	private KtNotificationManager<?> notificationManager;
	private KtCachingRequester cachingRequester;
	private UserKey userKey;
	private SingleUserKtRequester suRequester;
	private Permissions permissions;

	private EncryptionHelper encryptionHelper = new EncryptionHelper();

	public final EventBus eventBus = new EventBus("Kadcontrade");
	private File configPath;

	/**
	 * Default constructor. All LiteMods must have a default constructor. In general
	 * you should do very little in the mod constructor EXCEPT for initialising any
	 * non-game-interfacing components or performing sanity checking prior to
	 * initialisation
	 */
	public LiteModKadconTrade() {
	}

	/**
	 * getName() should be used to return the display name of your mod and MUST NOT
	 * return null
	 * 
	 * @see com.mumfrey.liteloader.LiteMod#getName()
	 */
	@Override
	public String getName() {
		return "Kadcontrade";
	}

	/**
	 * getVersion() should return the same version string present in the mod
	 * metadata, although this is not a strict requirement.
	 * 
	 * @see com.mumfrey.liteloader.LiteMod#getVersion()
	 */
	@Override
	public String getVersion() {
		return "0.0.1";
	}

	/**
	 * init() is called very early in the initialisation cycle, before the game is
	 * fully initialised, this means that it is important that your mod does not
	 * interact with the game in any way at this point.
	 * 
	 * @see com.mumfrey.liteloader.LiteMod#init(java.io.File)
	 */
	@Override
	public void init(File configPath) {
		this.configPath = configPath;
		LiteLoader.getInput().registerKeyBinding(ktKeyBinding);
	}

	/**
	 * upgradeSettings is used to notify a mod that its version-specific settings
	 * are being migrated
	 * 
	 * @see com.mumfrey.liteloader.LiteMod#upgradeSettings(java.lang.String,
	 *      java.io.File, java.io.File)
	 */
	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath) {
	}

	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) {
		if (Minecraft.isGuiEnabled() && inGame && minecraft.inGameHasFocus && ktKeyBinding.isPressed()) {
			if (getLoginState() != LoginState.SUCCESS) {
				minecraft.displayGuiScreen(new KtLoginScreen(this));
			} else {
				minecraft.displayGuiScreen(new KtOverviewScreen(this));
			}
		}
	}

	@Override
	public void onJoinGame(INetHandler netHandler, SPacketJoinGame joinGamePacket, ServerData serverData,
			RealmsServer realmsServer) {
//		setUser(new TokenUser(uid, tid, tkn)); TODO
	}

	public void usePassword(String password) {
		String tkn = encryptionHelper.aesDecrypt(encrTkn, password);
		if (tkn == null) {
			throw new IllegalArgumentException("wrong password");
		}
		setUser(new TokenUser(uid, tid, tkn), password);
	}

	public void loadP12(char[] password) throws IOException, GeneralSecurityException {
		File certificateFile = getCertificateCandidates()[0];
		try (InputStream keyInput = new FileInputStream(certificateFile)) {
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(keyInput, password);
			final Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				final Certificate certificate = keyStore.getCertificate(aliases.nextElement());
				if (certificate == null || !(certificate instanceof X509Certificate)) {
					continue;
				}
				final JcaX509CertificateHolder jcaX509CertificateHolder = new JcaX509CertificateHolder(
						(X509Certificate) certificate);
				final RDN[] rdn = jcaX509CertificateHolder.getSubject().getRDNs(BCStyle.UID);
				if (rdn.length != 1 || rdn[0].isMultiValued()) {
					continue;
				}
				String uid = IETFUtils.valueToString(rdn[0].getFirst().getValue());
				setUser(new CertificateUser(uid, certificateFile, password));
				return;
			}
			throw new IOException("no certificate found");
		}
	}

	private File[] getCertificateCandidates() {
		return configPath.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return "certificate.p12".equals(name);
			}
		});
	}

	private void setUser(CertificateUser user) {
		this.uid = user.getUserId();
		this.tid = "";
		this.tkn = "";
		this.encrTkn = null;
//		LiteLoader.getInstance().writeConfig(this);
		closeRequesters();

		userStore.add(user);
		userKey = user.getKey();
		requester = KtRetrofit.createClientCertificateRequester("miecraftmod", userStore, user.getKey(), null);
		buildDecoratingRequesters(userKey, requester);
		checkPermissions();
	}

	public void setUser(TokenUser user, String password) {
		this.uid = user.getUserId();
		this.tid = user.getTokenId();
		this.tkn = user.getToken();
		this.encrTkn = encryptionHelper.aesEncrypt(tkn, password);
		LiteLoader.getInstance().writeConfig(this);
		closeRequesters();

		userStore.add(user);
		userKey = user.getKey();
		requester = KtRetrofit.createDefaultRequester("miecraftmod", userStore, null);
		buildDecoratingRequesters(userKey, requester);
		checkPermissions();
	}

	private void buildDecoratingRequesters(UserKey key, KtRetrofitRequester requester) {
		final KtOkHttpWebsocket ws = new KtOkHttpWebsocket("wss://kt.125m125.de/api/websocket",
				requester.getOkHttpClient());
		notificationManager = new KtWebsocketNotificationHandler(userStore);
		KtWebsocketManager.builder(ws).addDefaultParsers().addListener(new OfflineMessageHandler())
				.addListener(new SessionHandler()).addListener(notificationManager)
				.addListener(new AutoReconnectionHandler()).buildAndOpen();
		cachingRequester = new KtSmartCache(requester, notificationManager);
		suRequester = new SingleUserKtRequester(key, cachingRequester); // TODO cachingRequester
	}

	private void checkPermissions() {
		suRequester.getPermissions().addCallback(new Callback<Permissions>() {

			@Override
			public void onSuccess(int status, Permissions result) {
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Kadcontrade verbunden."));
				permissions = result;
				setLoginState(LoginState.SUCCESS);
			}

			@Override
			public void onFailure(int status, String message, String humanReadableMessage) {
				if (status == -1) {
					Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
							"Kadcontrade funktioniert nicht mit der mitgelieferten JRE. Bitte stelle Minecraft so ein, dass es eine JRE>1.8u151 verwendet."));
					setLoginState(LoginState.ILLEGAL_JRE);
				} else {
					Minecraft.getMinecraft().player.sendMessage(
							new TextComponentString("Kadcontrade konnte keine gültigen Logindaten finden."));
					setLoginState(LoginState.FAILURE);
				}
			}

			@Override
			public void onError(Throwable t) {
				Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
						"Bei der Verbindung mit Kadcontrade ist ein unbekannter Fehler aufgetreten."));
				setLoginState(LoginState.FAILURE);
				t.printStackTrace();
			}
		});
	}

	private void closeRequesters() {
		if (suRequester != null)
			try {
				suRequester.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		if (cachingRequester != null)
			try {
				cachingRequester.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		if (notificationManager != null)
			notificationManager.disconnect();
		if (requester != null)
			requester.close();
	}

	public UserKey getUser() {
		return userKey;
	}

	public SingleUserKtRequester getRequester() {
		return suRequester;
	}

	public Permissions getCurrentPermissions() {
		if (state == LoginState.SUCCESS)
			return permissions;
		else
			return Permissions.NO_PERMISSIONS;
	}

	public LoginState getLoginState() {
		return state;
	}

	public void setLoginState(LoginState state) {
		this.state = state;
		eventBus.post(state);
		if (state == LoginState.SUCCESS) {
			if (permissions.mayReadItems())
				notificationManager.subscribeToItems(
						m -> Minecraft.getMinecraft().player
								.sendMessage(new TextComponentString("Dein Itemstand hat sich verändert.")),
						userKey, false);
			if (permissions.mayReadPayouts())
				notificationManager.subscribeToPayouts(
						m -> Minecraft.getMinecraft().player
								.sendMessage(new TextComponentString("Eine Auszahlungen wurde bearbeitet.")),
						userKey, false);
			if (permissions.mayReadOrders())
				notificationManager.subscribeToTrades(
						m -> Minecraft.getMinecraft().player.sendMessage(
								new TextComponentString("Für eine Order wurde ein Gegenangebot gefunden.")),
						userKey, false);
		}
	}

	@Override
	public void onShutDown() {
		closeRequesters();
	}

	public KtNotificationManager<?> getNotificationManager() {
		return notificationManager;
	}

	public boolean hasEncryptedTkn() {
		return encrTkn != null;
	}

	public boolean p12Present() {
		return getCertificateCandidates().length > 0;
	}

}
