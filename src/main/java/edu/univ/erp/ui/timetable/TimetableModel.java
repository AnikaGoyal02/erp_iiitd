package edu.univ.erp.ui.timetable;

import edu.univ.erp.domain.Section;
import java.time.LocalTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimetableModel {

  private static final Logger log = LoggerFactory.getLogger(TimetableModel.class);

  public static final List<String> DAYS = List.of("Mon", "Tue", "Wed", "Thu", "Fri");
  private final List<LocalTime> slotStarts;
  private final Map<Integer, Map<String, String>> grid;
  private final LocalTime earliest;
  private final LocalTime latest;

  public TimetableModel(List<Section> sections) {
    log.debug("Initializing TimetableModel with {} sections", (sections == null ? 0 : sections.size()));

    if (sections == null)
      sections = List.of();

    LocalTime min = null;
    LocalTime max = null;
    List<TimeSlotParser.Parsed> parsedList = new ArrayList<>();

    for (Section sec : sections) {
      try {
        log.debug("Parsing time slot for sectionId={} dayTime={}", sec.getSectionId(), sec.getDayTime());
        var p = TimeSlotParser.parse(sec.getDayTime());
        parsedList.add(p);

        if (min == null || p.start.isBefore(min))
          min = p.start;
        if (max == null || p.end.isAfter(max))
          max = p.end;

      } catch (Exception ex) {
        log.warn("Skipping unparsable time slot for sectionId={} ({})", sec.getSectionId(), sec.getDayTime());
      }
    }

    if (min == null)
      min = LocalTime.of(8, 0);
    if (max == null)
      max = LocalTime.of(18, 0);

    log.debug("Earliest class={}, Latest class={}", min, max);

    LocalTime startSlot = min.minusMinutes(30);
    if (startSlot.isBefore(LocalTime.of(0, 0)))
      startSlot = LocalTime.of(0, 0);
    LocalTime endSlot = max.plusMinutes(30);
    if (endSlot.isAfter(LocalTime.of(23, 59)))
      endSlot = LocalTime.of(23, 30);

    this.earliest = startSlot;
    this.latest = endSlot;

    log.debug("Grid start={}, end={}", earliest, latest);

    slotStarts = new ArrayList<>();
    LocalTime t = startSlot;
    while (!t.isAfter(endSlot.minusMinutes(0))) {
      slotStarts.add(t);
      t = t.plusMinutes(30);
      if (t.equals(LocalTime.MIDNIGHT))
        break;
    }

    log.debug("Total time slots generated: {}", slotStarts.size());

    grid = new HashMap<>();
    for (int r = 0; r < slotStarts.size(); r++)
      grid.put(r, new HashMap<>());

    for (Section sec : sections) {
      try {
        var p = TimeSlotParser.parse(sec.getDayTime());
        fillSectionInGrid(sec, p);
      } catch (Exception ex) {
        log.warn("Failed to place sectionId={} in grid", sec.getSectionId());
      }
    }

    log.info("TimetableModel initialized successfully with {} sections", sections.size());
  }

  private void fillSectionInGrid(Section sec, TimeSlotParser.Parsed p) {
    log.debug("Filling grid for sectionId={} days={} start={} end={}",
              sec.getSectionId(), p.days, p.start, p.end);

    int startRow = findRowIndex(p.start);
    int endRow = findRowIndexExclusive(p.end);
    if (startRow < 0 || endRow <= startRow) {
      log.warn("Invalid grid placement for sectionId={} startRow={} endRow={}",
               sec.getSectionId(), startRow, endRow);
      return;
    }

    String text = sec.getCourseTitle() + ", " + "Room: " + sec.getRoom();

    for (int r = startRow; r < endRow; r++) {
      for (String day : p.days) {
        if (!DAYS.contains(day))
          continue;
        grid.get(r).put(day, mergeText(grid.get(r).get(day), text));
      }
    }

    log.debug("SectionId={} placed into grid successfully", sec.getSectionId());
  }

  private String mergeText(String oldVal, String newVal) {
    if (oldVal == null)
      return newVal;
    if (oldVal.contains(newVal))
      return oldVal;
    return oldVal + "\n" + newVal;
  }

  private int findRowIndex(LocalTime time) {
    for (int i = 0; i < slotStarts.size(); i++) {
      LocalTime s = slotStarts.get(i);
      if (!time.isAfter(s.plusMinutes(29))) {
        return i;
      }
      if (time.equals(s))
        return i;
      if (time.isBefore(s))
        return i;
    }
    return slotStarts.size() - 1;
  }

  private int findRowIndexExclusive(LocalTime time) {
    for (int i = 0; i < slotStarts.size(); i++) {
      LocalTime s = slotStarts.get(i);
      if (!s.isBefore(time))
        return i;
    }
    return slotStarts.size();
  }

  public List<LocalTime> getSlotStarts() {
    return slotStarts;
  }

  public String getCell(int row, String day) {
    var m = grid.get(row);
    if (m == null)
      return "";
    return m.getOrDefault(day, "");
  }

  public LocalTime getEarliest() {
    return earliest;
  }

  public LocalTime getLatest() {
    return latest;
  }
}
