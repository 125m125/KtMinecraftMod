package de._125m125.kt.minecraft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.lwjgl.input.Keyboard;
import org.slf4j.LoggerFactory;
import org.xml.sax.ext.LexicalHandler;

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
import de._125m125.kt.ktapi.core.KtRequester;
import de._125m125.kt.ktapi.core.NotificationListener;
import de._125m125.kt.ktapi.core.SingleUserKtRequester;
import de._125m125.kt.ktapi.core.entities.Permissions;
import de._125m125.kt.ktapi.core.results.Callback;
import de._125m125.kt.ktapi.core.users.KtUserStore;
import de._125m125.kt.ktapi.core.users.TokenUser;
import de._125m125.kt.ktapi.core.users.TokenUserKey;
import de._125m125.kt.ktapi.retrofit.KtRetrofit;
import de._125m125.kt.ktapi.smartCache.KtCachingRequesterIml;
import de._125m125.kt.ktapi.websocket.KtWebsocketManager;
import de._125m125.kt.ktapi.websocket.events.MessageReceivedEvent;
import de._125m125.kt.ktapi.websocket.events.listeners.AutoReconnectionHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.KtWebsocketNotificationHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.OfflineMessageHandler;
import de._125m125.kt.ktapi.websocket.events.listeners.SessionHandler;
import de._125m125.kt.ktapi.websocket.okhttp.KtOkHttpWebsocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.util.text.TextComponentString;
import okhttp3.logging.HttpLoggingInterceptor.Level;

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
	private String tkn = "";

	private LoginState state;

	private KtUserStore userStore = new KtUserStore();
	private KtRequester<TokenUserKey> requester;
	private KtNotificationManager<TokenUserKey> notificationManager;
	private KtCachingRequester<TokenUserKey> cachingRequester;
	private TokenUserKey userKey;
	private SingleUserKtRequester<TokenUserKey> suRequester;
	private Permissions permissions;
	private List<CompletableFuture<NotificationListener>> listeners = new ArrayList<>();

	public final EventBus eventBus = new EventBus("Kadcontrade");

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
		LiteLoader.getInput().registerKeyBinding(ktKeyBinding);
		requester = KtRetrofit.createDefaultRequester(userStore, null);
		final KtOkHttpWebsocket ws = new KtOkHttpWebsocket("wss://kt.125m125.de/api/websocket");
		notificationManager = new KtWebsocketNotificationHandler<TokenUserKey>(userStore);
		KtWebsocketManager.builder(ws).addDefaultParsers().addListener(new OfflineMessageHandler())
				.addListener(new SessionHandler()).addListener(notificationManager)
				.addListener(new AutoReconnectionHandler()).buildAndOpen();
		cachingRequester = new KtCachingRequesterIml<>(requester, notificationManager);
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
			minecraft.displayGuiScreen(new KtOverviewScreen(this));
		}
	}

	@Override
	public void onJoinGame(INetHandler netHandler, SPacketJoinGame joinGamePacket, ServerData serverData,
			RealmsServer realmsServer) {
		setUser(new TokenUser(uid, tid, tkn));
	}
	
	public void setUser(TokenUser user) {
		this.uid = user.getUserId();
		this.tid = user.getTokenId();
		this.tkn = user.getToken();
		LiteLoader.getInstance().writeConfig(this);
		userStore.add(user);
		userKey = user.getKey();
		suRequester = new SingleUserKtRequester<>(userKey, cachingRequester); // TODO cachingRequester
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
							"Kadcontrade funktioniert nicht mit dem mitgelieferten JRE. Bitte stelle Minecraft so ein, dass es eine JRE>1.8u160 verwendet."));
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

	public TokenUserKey getUser() {
		return userKey;
	}

	public SingleUserKtRequester<TokenUserKey> getRequester() {
		return suRequester;
	}

	public Permissions getCurrentPermissions() {
		if (state == LoginState.SUCCESS)
			return permissions;
		else
			return new Permissions();
	}

	public LoginState getLoginState() {
		return state;
	}

	public void setLoginState(LoginState state) {
		listeners.forEach(notificationManager::unsubscribe);
		this.state = state;
		eventBus.post(state);
		if (state == LoginState.SUCCESS) {
			notificationManager.subscribeToItems(m->Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
						"Dein Itemstand hat sich verändert.")), userKey, false);
			notificationManager.subscribeToPayouts(m -> Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
					"Eine Auszahlungen wurde bearbeitet.")), userKey, false);
			notificationManager.subscribeToTrades(m->Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
					"Für eine Order wurde ein Gegenangebot gefunden.")), userKey, false);
		}
	}

	@Override
	public void onShutDown() {
		System.out.println(notificationManager);
		try {
			cachingRequester.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println(notificationManager);
		notificationManager.disconnect();
		try {
			requester.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public KtNotificationManager<TokenUserKey> getNotificationManager() {
		return notificationManager;
	}

}
