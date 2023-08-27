package games.strategy.triplea.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.Matches;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;

/**
 * An attachment for instances of {@link Territory} that defines a canal through which certain land
 * units may pass from one bordering territory to another. Note: Empty collection fields default to
 * null to minimize memory use and serialization size.
 */
public class CanalAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -1991066817386812634L;

  private String canalName = "";
  private @Nullable Set<Territory> landTerritories = null;
  private @Nullable Set<UnitType> excludedUnits = null;
  private boolean canNotMoveThroughDuringCombatMove = false;

  public CanalAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Set<CanalAttachment> getByCanalName(final Territory t, final String canalName) {
    return get(t, canalAttachment -> canalAttachment.getCanalName().equals(canalName));
  }

  public static Set<CanalAttachment> get(final Territory t, final Route onRoute) {
    return get(t, attachment -> isCanalOnRoute(attachment.getCanalName(), onRoute));
  }

  private static Set<CanalAttachment> get(final Territory t, Predicate<CanalAttachment> cond) {
    return t.getAttachments().values().stream()
        .filter(attachment -> attachment.getName().startsWith(Constants.CANAL_ATTACHMENT_PREFIX))
        .map(CanalAttachment.class::cast)
        .filter(cond)
        .collect(Collectors.toSet());
  }

  /**
   * Checks if the route contains both territories to pass through the given canal. If route is null
   * returns true.
   */
  private static boolean isCanalOnRoute(final String canalName, final Route route) {
    boolean previousTerritoryHasCanal = false;
    for (final Territory t : route) {
      boolean currentTerritoryHasCanal = !getByCanalName(t, canalName).isEmpty();
      if (previousTerritoryHasCanal && currentTerritoryHasCanal) {
        return true;
      }
      previousTerritoryHasCanal = currentTerritoryHasCanal;
    }
    return false;
  }


  static CanalAttachment get(final Territory t, final String nameOfAttachment) {
    return getAttachment(t, nameOfAttachment, CanalAttachment.class);
  }

  private void setCanalName(final String name) {
    canalName = name.intern();
  }

  public String getCanalName() {
    return canalName;
  }

  private void resetCanalName() {
    canalName = "";
  }

  private void setLandTerritories(final String landTerritories) {
    final Set<Territory> terrs = new HashSet<>();
    for (final String name : splitOnColon(landTerritories)) {
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new IllegalStateException("Canals: No territory called: " + name + thisErrorMsg());
      }
      terrs.add(territory);
    }
    this.landTerritories = terrs;
  }

  private void setLandTerritories(final Set<Territory> value) {
    landTerritories = value;
  }

  public Set<Territory> getLandTerritories() {
    return getSetProperty(landTerritories);
  }

  private void resetLandTerritories() {
    landTerritories = null;
  }

  private void setExcludedUnits(final String value) {
    if (excludedUnits == null) {
      excludedUnits = new HashSet<>();
    }
    if (value.equalsIgnoreCase("NONE")) {
      return;
    }
    if (value.equalsIgnoreCase("ALL")) {
      excludedUnits.addAll(getData().getUnitTypeList().getAllUnitTypes());
      return;
    }
    for (final String name : splitOnColon(value)) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(name);
      if (ut == null) {
        throw new IllegalStateException("Canals: No UnitType called: " + name + thisErrorMsg());
      }
      excludedUnits.add(ut);
    }
  }

  private void setExcludedUnits(final Set<UnitType> value) {
    excludedUnits = value;
  }

  public Set<UnitType> getExcludedUnits() {
    if (excludedUnits == null) {
      return new HashSet<>(
          CollectionUtils.getMatches(
              getData().getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAir()));
    }
    return excludedUnits;
  }

  private void resetExcludedUnits() {
    excludedUnits = null;
  }

  private void setCanNotMoveThroughDuringCombatMove(final boolean value) {
    canNotMoveThroughDuringCombatMove = value;
  }

  public boolean getCanNotMoveThroughDuringCombatMove() {
    return canNotMoveThroughDuringCombatMove;
  }

  @Override
  public void validate(final GameState data) throws GameParseException {
    if (canalName.isEmpty()) {
      throw new GameParseException("Canals must have a canalName set!" + thisErrorMsg());
    }
    if (getLandTerritories().isEmpty()) {
      throw new GameParseException(
          "Canal named " + canalName + " must have landTerritories set!" + thisErrorMsg());
    }
    final Set<Territory> territories = new HashSet<>();
    for (final Territory t : data.getMap()) {
      if (!getByCanalName(t, canalName).isEmpty()) {
        territories.add(t);
      }
    }
    if (territories.size() != 2) {
      throw new GameParseException(
          "Wrong number of sea zones for canal (exactly 2 sea zones may have the same canalName):"
              + territories);
    }
  }

  @Override
  public MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "canalName":
        return MutableProperty.ofString(
            this::setCanalName, this::getCanalName, this::resetCanalName);
      case "landTerritories":
        return MutableProperty.of(
            this::setLandTerritories,
            this::setLandTerritories,
            this::getLandTerritories,
            this::resetLandTerritories);
      case "excludedUnits":
        return MutableProperty.of(
            this::setExcludedUnits,
            this::setExcludedUnits,
            this::getExcludedUnits,
            this::resetExcludedUnits);
      case "canNotMoveThroughDuringCombatMove":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool,
            this::setCanNotMoveThroughDuringCombatMove,
            this::getCanNotMoveThroughDuringCombatMove,
            () -> false);
      default:
        return null;
    }
  }
}
