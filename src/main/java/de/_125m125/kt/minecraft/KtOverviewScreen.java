package de._125m125.kt.minecraft;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Mouse;

import de._125m125.kt.ktapi.core.entities.Item;
import de._125m125.kt.ktapi.core.entities.ItemName;
import de._125m125.kt.ktapi.core.entities.Message;
import de._125m125.kt.ktapi.core.entities.Permissions;
import de._125m125.kt.ktapi.core.results.Callback;
import de._125m125.kt.ktapi.core.results.WriteResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class KtOverviewScreen extends GuiScreen {
	private static final int ITEMBUTTON_ID_OFFSET = 100;

	private Map<Integer, GuiTextField> fields = new HashMap<>();
	private List<GuiButton> itemButtons = new ArrayList<>();

	private GuiLabel userStatusLabel;
	private GuiLabel messageLabel;
	private LiteModKadconTrade kt;

	private int currentScroll = 0;

	public KtOverviewScreen(LiteModKadconTrade kt) {
		this(kt, 0);
	}

	public KtOverviewScreen(LiteModKadconTrade kt, int offset) {
		this.kt = kt;
		currentScroll = offset;
	}

	@Override
	public void initGui() {
		super.initGui();
		kt.eventBus.register(this);
		allowUserInput = true;

		updateMessages();

		for (int i = 0; i < 5; i++) {
			itemButtons.add(addButton(
					new GuiButton(ITEMBUTTON_ID_OFFSET + i, this.width / 8, 100 + i * 25, this.width * 3 / 4, 20, "")));
		}
		addButton(new GuiButton(99, this.width / 4, 225, this.width / 2, 20, "Kontoauszug auslesen"));
		scroll(0);
	}

	private void updateMessages() {
		if (messageLabel != null) {
			labelList.remove(messageLabel);
		}
		if (!kt.getCurrentPermissions().mayReadMessages()) {
			return;
		}
		kt.getRequester().getMessages(0, 10).addCallback(new Callback<List<Message>>() {
			@Override
			public void onSuccess(int status, List<Message> result) {
				messageLabel = new GuiLabel(fontRenderer, 0, width / 16, 28, width * 7 / 8, 50, 0xFFFFFF);
				for (int i = 0; i < result.size(); i++)
					messageLabel.addLine(result.get(i).getTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
							+ " " + result.get(i).getMessage());
				labelList.add(messageLabel);
			}

			@Override
			public void onFailure(int status, String message, String humanReadableMessage) {
				System.out.println("message failure " + status+": "+message);
			}

			@Override
			public void onError(Throwable t) {
				System.out.println("message error " + t);
				t.printStackTrace();
			}
		});
	}

	private void setGuiLabelText(String text) {
		if (userStatusLabel != null) {
			labelList.remove(userStatusLabel);
		}
		userStatusLabel = new GuiLabel(fontRenderer, 0, this.width / 2 + this.width / 8, 26, this.width / 4, 20,
				0xFFFFFF);
		userStatusLabel.addLine(text);
		labelList.add(userStatusLabel);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		fields.values().forEach(t -> t.mouseClicked(mouseX, mouseY, mouseButton));
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		fields.values().stream().filter(GuiTextField::isFocused).forEach(t -> t.textboxKeyTyped(typedChar, keyCode));
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		fields.values().forEach(GuiTextField::drawTextBox);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void handleMouseInput() throws IOException {
		int signum = Integer.signum(Mouse.getEventDWheel());
		if (signum != 0)
			scroll(-signum);
		super.handleMouseInput();
	}

	private void scroll(int signum) {
		Permissions currentPermissions = kt.getCurrentPermissions();
		if (currentPermissions.mayReadItems()) {
			kt.getRequester().getItems().addCallback(new Callback<List<Item>>() {

				@Override
				public void onSuccess(int status, List<Item> result) {
					currentScroll = Math.max(0, Math.min(result.size() - itemButtons.size(), currentScroll + signum));
					for (int i = 0; i < itemButtons.size(); i++) {
						itemButtons.get(i).displayString = result.get(currentScroll + i).getName() + ": "
								+ ("-1".equals(result.get(currentScroll + i).getId())
										? result.get(currentScroll + i).getAmount()
										: (long) result.get(currentScroll + i).getAmount());
					}
				}

				@Override
				public void onFailure(int status, String message, String humanReadableMessage) {
					System.out.println("failure... " + status + " " + message + " " + humanReadableMessage);
				}

				@Override
				public void onError(Throwable t) {
					t.printStackTrace();
				}
			});
		} else {
			System.out.println("reading item alternative");
			kt.getRequester().getItemNames().addCallback(new Callback<List<ItemName>>() {

				@Override
				public void onError(Throwable t) {
					t.printStackTrace();
				}

				@Override
				public void onFailure(int status, String message, String humanReadableMessage) {
					System.out.println("failure... " + status + " " + message + " " + humanReadableMessage);
				}

				@Override
				public void onSuccess(int status, List<ItemName> result) {
					currentScroll = Math.max(0, Math.min(result.size() - itemButtons.size(), currentScroll + signum));
					for (int i = 0; i < itemButtons.size(); i++) {
						itemButtons.get(i).displayString = result.get(currentScroll + i).getName();
					}
				}
			});
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id > ITEMBUTTON_ID_OFFSET && button.id < ITEMBUTTON_ID_OFFSET * 2) {
			int item = button.id - ITEMBUTTON_ID_OFFSET + currentScroll;
			if (kt.getCurrentPermissions().mayReadItems())
				kt.getRequester().getItems().addCallback(new Callback<List<Item>>() {
					@Override
					public void onSuccess(int status, List<Item> result) {
						Minecraft.getMinecraft().displayGuiScreen(
								new KtDetailScreen<>(kt, kt.getNotificationManager(), result.get(item), currentScroll));
					}

					@Override
					public void onFailure(int status, String message, String humanReadableMessage) {
						System.out.println("failure... " + status + " " + message + " " + humanReadableMessage);
					}

					@Override
					public void onError(Throwable t) {
						t.printStackTrace();
					}
				});
			else
				kt.getRequester().getItemNames().addCallback(new Callback<List<ItemName>>() {
					@Override
					public void onSuccess(int status, List<ItemName> result) {
						Minecraft.getMinecraft().displayGuiScreen(new KtDetailScreen<>(kt, kt.getNotificationManager(),
								new Item(result.get(item).getId(), result.get(item).getName(), -1), currentScroll));
					}

					@Override
					public void onFailure(int status, String message, String humanReadableMessage) {
						System.out.println("failure... " + status + " " + message + " " + humanReadableMessage);
					}

					@Override
					public void onError(Throwable t) {
						t.printStackTrace();
					}
				});
		} else if (button.id == 99) {
			kt.getRequester().readBankStatement().addCallback(new Callback<WriteResult<Long>>() {

				@Override
				public void onError(Throwable t) {
					t.printStackTrace();
				}

				@Override
				public void onFailure(int arg0, String arg1, String arg2) {
					button.displayString = "Kontoauszug noch nicht bereit.";
				}

				@Override
				public void onSuccess(int arg0, WriteResult<Long> arg1) {
					button.displayString = "Kontoauszug wird gleich ausgelesen.";
				}

			});
		} else {
			super.actionPerformed(button);
		}
	}

	@Override
	public void onGuiClosed() {
		kt.eventBus.unregister(this);
		super.onGuiClosed();
	}

}
