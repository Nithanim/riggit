package riggit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class DateUtil {
  public String dateToDifference(LocalDateTime dateTime) {
    LocalDateTime now = LocalDateTime.now();
    var p = Period.between(dateTime.toLocalDate(), now.toLocalDate());
    if (p.getDays() > 1) {
      return getLongTime(p);
    } else {
      var d = Duration.between(dateTime, now);
      if (d.toHoursPart() >= 24) {
        return getLongTime(p);
      } else {
        return getShortTime(d);
      }
    }
  }

  @NotNull
  private String getShortTime(Duration d) {
    if (d.toHoursPart() > 0) {
      return d.toHoursPart() + "h";
    } else if (d.toMinutesPart() > 0) {
      return d.toMinutesPart() + "min";
    } else {
      return d.toSecondsPart() + "s";
    }
  }

  @NotNull
  private String getLongTime(Period p) {
    if (p.getYears() > 0) {
      return p.getYears() + "y";
    } else if (p.getMonths() > 0) {
      return p.getMonths() + "mo";
    } else {
      return p.getDays() + "d";
    }
  }

  public LocalDateTime convertToLocalDateTime(Date dateToConvert) {
    return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }
}
