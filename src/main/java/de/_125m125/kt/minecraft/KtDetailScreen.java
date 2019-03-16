package de._125m125.kt.minecraft;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import de._125m125.kt.ktapi.core.BuySell;
import de._125m125.kt.ktapi.core.KtNotificationManager;
import de._125m125.kt.ktapi.core.PayoutType;
import de._125m125.kt.ktapi.core.entities.HistoryEntry;
import de._125m125.kt.ktapi.core.entities.Item;
import de._125m125.kt.ktapi.core.entities.Payout;
import de._125m125.kt.ktapi.core.entities.Trade;
import de._125m125.kt.ktapi.core.results.Callback;
import de._125m125.kt.ktapi.core.results.WriteResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class KtDetailScreen<T> extends GuiScreen {
	private static final int STATUS_LABEL = -1;
	private static final int ITEM_LABEL = 0;
	private static final int HISTORY_LABEL = 1;
	private static final int AMOUNT = 2; // to 3
	private static final int PRICE = 4; // to 5
	private static final int CREATE = 6; // to 7
	private static final int TAKEOUT = 8; // to 9
	private static final int CANCEL = 10; // to 11
	private static final int TRADE_HELP_TEXTS = 12; // to 15
	private static final int PAYOUT_AMOUNT = 16;
	private static final int PayoutType_FIELD = 17;
	private static final int PAYOUT_SUBMIT = 18;
	private static final int BACK = 19;
	private static final int PAYOUT_LABELS = 1000; // to infinity

	private static final int ITEM_START = 20;
	private static final int HISTORY_START = 40;
	private static final int TRADE_START = 60;
	private static final int PAYOUT_START = 180;

	private final long[] tradeIds = { 0, 0 };

	private LiteModKadconTrade kt;
	private Item item;
	private List<Payout> payouts;
	private boolean isKadis;
	private List<CompletableFuture<T>> listeners = new ArrayList<>();

	private Map<Integer, GuiLabel> labels = new HashMap<>();
	private Map<Integer, GuiTextField> textFields = new HashMap<>();
	private Map<Integer, EntrySpecifier> payoutButtons = new HashMap<>();
	private Callback<WriteResult<Trade>> writeCallback = new Callback<WriteResult<Trade>>() {

		@Override
		public void onSuccess(int status, WriteResult<Trade> result) {
			System.out.println("success   "+result);
			setStatusLabelText("Erfolg!");
		}

		@Override
		public void onFailure(int status, String message, String humanReadableMessage) {
			System.out.println("failure   "+message);
			setStatusLabelText("Fehlschlag: " + humanReadableMessage);
		}

		@Override
		public void onError(Throwable t) {
			System.out.println("error "+t);
			t.printStackTrace();
			setStatusLabelText("Unbekannter Fehler.");
		}

	};
	private KtNotificationManager<T> notificationManager;
	private int offset;

	private void setStatusLabelText(String string) {
		addLabel(new GuiLabel(fontRenderer, STATUS_LABEL, width / 8, 5, width * 3 / 4, 20, 0xFFFFFF), string);
	}

	public KtDetailScreen(LiteModKadconTrade kt, KtNotificationManager<T> manager, Item item, int offset) {
		this.kt = kt;
		notificationManager = manager;
		this.item = item;
		this.offset = offset;
		isKadis = "-1".equals(item.getId());
	}

	@Override
	public void initGui() {
		super.initGui();
		updateInventory();
		updateHistory();
		if (!isKadis) {
			updateOrders();
		}
		updatePayouts();
		if (kt.getCurrentPermissions().mayWritePayouts()) {
			addLabel(new GuiLabel(fontRenderer, -100, width / 8, PAYOUT_START, width / 8, 20, 0xFFFFFF), " Menge");
			textFields.put(PAYOUT_AMOUNT,
					new GuiTextField(PAYOUT_AMOUNT, fontRenderer, width / 8 * 2, PAYOUT_START, width / 8 * 2, 20));
			addLabel(new GuiLabel(fontRenderer, -101, width / 8 * 4, PAYOUT_START, width / 8, 20, 0xFFFFFF), " Art");
			textFields.put(PayoutType_FIELD,
					new GuiTextField(PayoutType_FIELD, fontRenderer, width / 8 * 5, PAYOUT_START, width / 8, 20));
			addButton(new GuiButton(PAYOUT_SUBMIT, width / 8 * 6 + 1, PAYOUT_START, width / 8, 20, "Auszahlen"));
		}
		addButton(new GuiButton(BACK, width * 3 / 4, ITEM_START, width / 8, 20, "Zurück"));
		GuiLabel guiLabel = new GuiLabel(fontRenderer, -8230, width / 8, PAYOUT_START-20, width*3 / 4, 20,
				0xFFFFFF);
		if (isKadis) {
			guiLabel.addLine("Auszahlungstyp: Kadcon->0");
		} else {
			guiLabel.addLine("Auszahlungstyp: Lieferung->0, Auszahlungsbox -> Nummer des Servers");
		}
		labelList.add(guiLabel);
		if (kt.getCurrentPermissions().mayReadItems()) {
			listeners.add(notificationManager.subscribeToItems(n -> updateInventory(), kt.getUser(), true));
			listeners.add(notificationManager.subscribeToItems(n -> updateInventory(), kt.getUser(), false));
		}
		if (kt.getCurrentPermissions().mayReadPayouts()) {
			listeners.add(notificationManager.subscribeToPayouts(n -> updatePayouts(), kt.getUser(), true));
			listeners.add(notificationManager.subscribeToPayouts(n -> updatePayouts(), kt.getUser(), false));
		}
		if (kt.getCurrentPermissions().mayReadOrders()) {
			listeners.add(notificationManager.subscribeToTrades(n -> updateOrders(), kt.getUser(), true));
			listeners.add(notificationManager.subscribeToTrades(n -> updateOrders(), kt.getUser(), false));
		}
		listeners.add(notificationManager.subscribeToHistory(n -> updateHistory()));
	}

	@Override
	public void onGuiClosed() {
		listeners.forEach(notificationManager::unsubscribe);
		super.onGuiClosed();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		textFields.values().forEach(tf -> tf.mouseClicked(mouseX, mouseY, mouseButton));
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		textFields.values().stream().filter(GuiTextField::isFocused)
				.forEach(tf -> tf.textboxKeyTyped(typedChar, keyCode));
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		textFields.values().forEach(GuiTextField::drawTextBox);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
		case CREATE:
			createTrade(0);
			break;
		case CREATE + 1:
			createTrade(1);
			break;
		case TAKEOUT:
			kt.getRequester().takeoutFromTrade(tradeIds[0]).addCallback(this.writeCallback);
			break;
		case TAKEOUT + 1:
			kt.getRequester().takeoutFromTrade(tradeIds[1]).addCallback(this.writeCallback);
			break;
		case CANCEL:
			kt.getRequester().cancelTrade(tradeIds[0]).addCallback(this.writeCallback);
			break;
		case CANCEL + 1:
			kt.getRequester().cancelTrade(tradeIds[1]).addCallback(this.writeCallback);
			break;
		case BACK:
			Minecraft.getMinecraft().displayGuiScreen(new KtOverviewScreen(kt, offset));
			break;
		case PAYOUT_SUBMIT:
			PayoutType type = null;
			switch (textFields.get(PayoutType_FIELD).getText()) {
			case "0":
				if (isKadis) {
					type = PayoutType.KADCON;
				} else {
					type = PayoutType.DELIVERY;
				}
				break;
			case "1":
				if (!isKadis) {
					type = PayoutType.PAYOUT_BOX_S1;
					break;
				}
			case "2":
				if (!isKadis) {
					type = PayoutType.PAYOUT_BOX_S2;
					break;
				}
			case "3":
				if (!isKadis) {
					type = PayoutType.PAYOUT_BOX_S3;
					break;
				}
			default:
				setStatusLabelText("Ungültiger Auszahlungstyp");
				return;
			}
			try {
				kt.getRequester().createPayout(kt.getUser(), type, item.getId(),
						textFields.get(PAYOUT_AMOUNT).getText().replaceAll(",", ".")).addCallback(new Callback<WriteResult<Payout>>() {

							@Override
							public void onError(Throwable t) {
								t.printStackTrace();
								setStatusLabelText("Fehlgeschlagen: Unbekannter Fehler.");
							}

							@Override
							public void onFailure(int status, String message, String hrm) {
								setStatusLabelText("Fehlgeschlagen: "+hrm);
							}

							@Override
							public void onSuccess(int status, WriteResult<Payout> result) {
								setStatusLabelText("Auszahlungsanfrage erfolgreich erstellt.");
							}
							
						});
			} catch (NumberFormatException e) {
				setStatusLabelText("Fehlgeschlagen: Ungültige Menge");
			}
			break;
		}
		EntrySpecifier entrySpecifier = payoutButtons.get(button.id);
		if (entrySpecifier != null) {
			payouts.stream().filter(p -> p.getMaterial().equals(item.getId()))
					.filter(p -> p.getState().equals(entrySpecifier.state))
					.filter(p -> p.getPayoutType().equals(entrySpecifier.payoutType)).forEach(p -> {
						if (p.getState().equals("Offen"))
							kt.getRequester().cancelPayout(kt.getUser(), p.getId());
						else
							kt.getRequester().takeoutPayout(kt.getUser(), p.getId());
					});
		}
		super.actionPerformed(button);
	}

	private void createTrade(int offset) {
		int amount;
		try {
			amount = Integer.parseInt(textFields.get(AMOUNT + offset).getText());
		} catch (NumberFormatException e) {
			setStatusLabelText("Fehlgeschlagen: Ungültige Menge");
			return;
		}
		double ppi;
		try {
			ppi = Double.parseDouble(textFields.get(PRICE + offset).getText().replaceAll(",", "."));
		} catch (NumberFormatException e) {
			setStatusLabelText("Fehlgeschlagen: Ungültiger Preis");
			return;
		}
		kt.getRequester().createTrade(offset == 0 ? BuySell.BUY : BuySell.SELL, item, amount, ppi)
				.addCallback(writeCallback);
	}

	private void updatePayouts() {
		if (kt.getCurrentPermissions().mayReadPayouts()) {
			kt.getRequester().getPayouts().addCallback(new Callback<List<Payout>>() {

				@Override
				public void onSuccess(int status, List<Payout> result) {
					payouts = result;
					labelList.removeIf(l -> l.id >= PAYOUT_LABELS);
					if (buttonList != null)
						buttonList.removeIf(l -> l.id >= PAYOUT_LABELS);
					payoutButtons.clear();

					TreeMap<EntrySpecifier, Double> collect = result.stream()
							.filter(p -> p.getMaterial().equals(item.getId())).collect(Collectors.groupingBy(
									EntrySpecifier::new, TreeMap::new, Collectors.summingDouble(Payout::getAmount)));
					int i = PAYOUT_START + 25;
					for (Entry<EntrySpecifier, Double> a : collect.entrySet()) {
						GuiLabel guiLabel = new GuiLabel(fontRenderer, PAYOUT_LABELS + i, width / 8, i, width / 2, 20,
								0xFFFFFF);
						guiLabel.addLine(a.getKey().payoutType + " von "
								+ (isKadis ? a.getValue() : a.getValue().longValue()) + ": " + a.getKey().state);
						labelList.add(guiLabel);
						if (kt.getCurrentPermissions().mayWritePayouts()) {
							if (a.getKey().state.equals("Offen")) {
								addButton(new GuiButton(i, width / 8 + width / 2, i, width / 4, 20, "Abbrechen"));
								payoutButtons.put(i, a.getKey());
								i += 25;
							} else if (a.getKey().state.equals("Fehlgeschlagen")) {
								addButton(new GuiButton(i, width / 8 + width / 2, i, width / 4, 20, "Entnehmen"));
								payoutButtons.put(i, a.getKey());
								i += 25;
							} else {
								i += 22;
							}
						} else {
							i += 22;
						}
					}
				}

				@Override
				public void onFailure(int status, String message, String humanReadableMessage) {
				}

				@Override
				public void onError(Throwable t) {
					t.printStackTrace();
				}
			});
		}
	}

	private void updateInventory() {
		if (kt.getCurrentPermissions().mayReadItems()) {
			kt.getRequester().getItems().addCallback(new Callback<List<Item>>() {
				@Override
				public void onSuccess(int status, List<Item> result) {
					GuiLabel guiLabel = new GuiLabel(fontRenderer, ITEM_LABEL, width / 8, ITEM_START, width * 3 / 4, 20,
							0xFFFFFF);
					if (isKadis) {
						guiLabel.addLine(item.getName() + ": " + item.getAmount());
					} else {
						guiLabel.addLine("Kadis (-1): "
								+ result.stream().filter(i -> "-1".equals(i.getId())).map(Item::getAmount)
										.map(Object::toString).findFirst().orElse("???")
								+ "     " + item.getName() + ": " + (long) item.getAmount());
					}
					GuiLabel put = labels.put(ITEM_LABEL, guiLabel);
					if (put != null) {
						labelList.remove(put);
					}
					labelList.add(guiLabel);
				}

				@Override
				public void onFailure(int status, String message, String humanReadableMessage) {
				}

				@Override
				public void onError(Throwable t) {
					t.printStackTrace();
				}
			});
		}
	}

	private void updateHistory() {
		kt.getRequester().getHistory(item, 30, 0).addCallback(new Callback<List<HistoryEntry>>() {
			@Override
			public void onSuccess(int status, List<HistoryEntry> result) {
				GuiLabel guiLabel = new GuiLabel(fontRenderer, HISTORY_LABEL, width / 8, HISTORY_START, width * 3 / 4,
						20, 0xFFFFFF);
				if (!result.isEmpty()) {
					guiLabel.addLine("Aktueller Preis: " + result.get(0).getClose() + "  " + percentageString(7, result)
							+ "  " + percentageString(30, result));
				} else {
					guiLabel.addLine("Für dieses Item gibt es noch keine Statistiken.");
				}
				GuiLabel put = labels.put(HISTORY_LABEL, guiLabel);
				if (put != null) {
					labelList.remove(put);
				}
				labelList.add(guiLabel);
			}

			@Override
			public void onFailure(int status, String message, String humanReadableMessage) {
			}

			@Override
			public void onError(Throwable t) {
				t.printStackTrace();
			}
		});
	}

	private void updateOrders() {
		if (!kt.getCurrentPermissions().mayReadOrders()) {
			updateEmptyTrade(0);
			updateEmptyTrade(1);
			return;
		}
		kt.getRequester().getTrades().addCallback(new Callback<List<Trade>>() {
			@Override
			public void onSuccess(int status, List<Trade> result) {
				boolean[] findings = { false, false };
				System.out.println(result);
				result.stream().filter(t -> t.getMaterialId().equals(item.getId())).forEach(t -> {
					if (t.isBuy()) {
						updateTrade(t, 0);
						findings[0] = true;
					} else {
						updateTrade(t, 1);
						findings[1] = true;
					}
				});
				if (!findings[0]) {
					updateEmptyTrade(0);
				}
				if (!findings[1]) {
					updateEmptyTrade(1);
				}
			}

			@Override
			public void onFailure(int status, String message, String humanReadableMessage) {
			}

			@Override
			public void onError(Throwable t) {
				t.printStackTrace();
			}

		});
	}

	private void updateTrade(Trade t, int offset) {
		buttonList.removeIf(b -> b.id == CREATE + offset);
		textFields.remove(AMOUNT + offset);
		textFields.remove(PRICE + offset);
		GuiLabel remove = labels.remove(TRADE_HELP_TEXTS + offset * 2);
		if (remove != null)
			labelList.remove(remove);
		remove = labels.remove(TRADE_HELP_TEXTS + 1 + offset * 2);
		if (remove != null)
			labelList.remove(remove);

		tradeIds[offset] = t.getId();

		GuiLabel amount = addLabel(new GuiLabel(fontRenderer, AMOUNT + offset, width / 8, TRADE_START + offset * 50,
				3 * width / 16, 20, 0xFFFFFF));
		amount.addLine("Aktuell " + t.getSold() + " von " + t.getAmount() + " für jeweils " + t.getPrice()
				+ (offset == 0 ? " gekauft" : " verkauft." + (t.getSold() == t.getAmount() ? "(Abgeschlossen)" : "")));
		if (!kt.getCurrentPermissions().mayWriteOrders())
			return;
		buttonList.removeIf(b -> b.id == CANCEL + offset);
		buttonList.removeIf(b -> b.id == TAKEOUT + offset);
		addButton(
				new GuiButton(CANCEL + offset, width / 8, TRADE_START + 20 + offset * 50, width / 8, 20, "Abbrechen"));
		addButton(new GuiButton(TAKEOUT + offset, width / 4, TRADE_START + 20 + offset * 50, width * 5 / 8, 20,
				Double.toString(t.getToTakeMoney() / 1000000) + " Kadis und " + Integer.toString(t.getToTakeItems())
						+ " " + item.getName() + " entnehmen"));
	}

	private GuiLabel addLabel(GuiLabel label, String... text) {
		GuiLabel put = labels.put(label.id, label);
		if (put != null)
			labelList.remove(put);
		labelList.add(label);
		for (String s : text) {
			label.addLine(s);
		}
		return label;
	}

	private void updateEmptyTrade(int offset) {
		if (!kt.getCurrentPermissions().mayWriteOrders())
			return;
		if (textFields.get(AMOUNT + offset) != null)
			return;
		if (buttonList != null) {
			buttonList.removeIf(b -> b.id == CANCEL + offset);
			buttonList.removeIf(b -> b.id == TAKEOUT + offset);
		}
		GuiLabel remove = labels.remove(AMOUNT + offset);
		if (remove != null) {
			labelList.remove(remove);
		}

		tradeIds[offset] = 0;

		// TODO: fill in current optimal orderbook values
		addLabel(new GuiLabel(fontRenderer, TRADE_HELP_TEXTS + offset * 2, width / 8, TRADE_START + offset * 50,
				width / 8, 20, 0xFFFFFF), " Anzahl");
		GuiTextField amountTf = new GuiTextField(AMOUNT + offset, fontRenderer, 2 * width / 8,
				TRADE_START + offset * 50, width / 4, 20);
		textFields.put(AMOUNT + offset, amountTf);
		addLabel(new GuiLabel(fontRenderer, TRADE_HELP_TEXTS + 1 + offset * 2, width / 2, TRADE_START + offset * 50,
				width / 8, 20, 0xFFFFFF), " Preis");
		GuiTextField priceTf = new GuiTextField(PRICE + offset, fontRenderer, width * 5 / 8, TRADE_START + offset * 50,
				width / 4, 20);
		textFields.put(PRICE + offset, priceTf);
		addButton(new GuiButton(CREATE + offset, width / 8, TRADE_START + 25 + offset * 50, width * 3 / 4, 20,
				offset == 0 ? "Kaufen" : "Verkaufen"));
	}

	private String percentageString(int days, List<HistoryEntry> result) {
		if (result.size() < days) {
			return "";
		}
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getPercentInstance();
		formatter.setMaximumFractionDigits(2);
		formatter.setPositivePrefix("+");
		formatter.setGroupingUsed(false);
		double change = (result.get(0).getClose() - result.get(days - 1).getOpen()) / result.get(0).getClose();
		return days + " Tage: " + formatter.format(change * 100).replaceAll("%", "%%");
	}

	private static class EntrySpecifier implements Comparable<EntrySpecifier> {
		private static Map<String, Integer> stateOrdering = new HashMap<>();
		static {
			stateOrdering.put("Offen", -100);
			stateOrdering.put("In Bearbeitung", -500);
			stateOrdering.put("Fehlgeschlagen", -200);
		}

		private String state;
		private String payoutType;

		public EntrySpecifier(Payout p) {
			this(p.getState(), p.getPayoutType());
		}

		public EntrySpecifier(String state, String payoutType) {
			super();
			this.state = state;
			this.payoutType = payoutType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((payoutType == null) ? 0 : payoutType.hashCode());
			result = prime * result + ((state == null) ? 0 : state.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EntrySpecifier other = (EntrySpecifier) obj;
			if (payoutType == null) {
				if (other.payoutType != null)
					return false;
			} else if (!payoutType.equals(other.payoutType))
				return false;
			if (state == null) {
				if (other.state != null)
					return false;
			} else if (!state.equals(other.state))
				return false;
			return true;
		}

		@Override
		public int compareTo(EntrySpecifier o) {
			int a = stateOrdering.getOrDefault(this.state, 0).compareTo(stateOrdering.getOrDefault(o.state, 0));
			if (a != 0) {
				return a;
			}
			return this.payoutType.compareTo(o.payoutType);
		}

	}

}
