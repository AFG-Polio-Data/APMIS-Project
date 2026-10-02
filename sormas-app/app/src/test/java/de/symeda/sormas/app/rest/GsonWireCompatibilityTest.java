package de.symeda.sormas.app.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Executable specification of what a DEPLOYED apk tolerates when the server changes a shared DTO.
 *
 * <p>
 * The mobile app and the server share {@code sormas-api}, so they compile against the same DTOs. That is
 * compile-time coupling only. Apks are distributed independently and devices in the field can run an old
 * build for a long time, so in practice an apk compiled against an OLDER {@code sormas-api} is regularly
 * parsing payloads from a NEWER server.
 *
 * <p>
 * These tests pin down what actually happens in that situation, against the real production Gson
 * configuration from {@link RetroProvider#initGson()}. The DTO and enum below deliberately do NOT reference
 * the live {@code sormas-api} types: they model the narrower view an already-shipped apk was compiled with,
 * which is the whole point.
 *
 * <p>
 * The headline result is that almost every incompatible change is <b>silent</b>. Removed fields, renamed
 * fields and unknown enum constants all deserialise to null rather than raising an error, so a device keeps
 * running and simply loses the value. Only certain type changes fail loudly. This is why these breaks cannot
 * be caught at runtime or reported from the field, and have to be gated at build time.
 *
 * <p>
 * If a change here makes one of these tests fail, the Gson configuration's compatibility behaviour has
 * changed. That may well be desirable, but it should be a deliberate decision rather than a side effect.
 */
public class GsonWireCompatibilityTest {

	/**
	 * {@code AgeGroup} as an already-deployed apk knows it. The live enum in sormas-api has more constants;
	 * this models the older, narrower view so version skew can be reproduced.
	 */
	enum DeployedAgeGroup {
		AGE_0_4,
		AGE_5_10,
		AGE_4_23M,
		AGE_10_14
	}

	/** A sync DTO as compiled into an apk that is already in the field. */
	static class DeployedDto {

		String uuid;
		DeployedAgeGroup ageGroup;
		Integer population;
		Date reportDate;
	}

	private final Gson gson = RetroProvider.initGson();

	private DeployedDto parse(String json) {
		return gson.fromJson(json.replace('\'', '"'), DeployedDto.class);
	}

	@Test
	public void addedFieldFromNewerServerIsIgnored() {

		// Additive server changes are safe: Gson skips properties the deployed apk does not know about.
		DeployedDto dto = parse("{'uuid':'A1','ageGroup':'AGE_4_23M','population':120,'newCategory':'Monitoring'}");

		assertThat(dto.uuid, is("A1"));
		assertThat(dto.population, is(120));
		assertThat(dto.ageGroup, is(DeployedAgeGroup.AGE_4_23M));
	}

	@Test
	public void removedFieldSilentlyBecomesNull() {

		// The server dropping a field does not fail. The value simply goes missing on the device.
		DeployedDto dto = parse("{'uuid':'A1','ageGroup':'AGE_4_23M'}");

		assertThat(dto.uuid, is("A1"));
		assertThat(dto.population, is(nullValue()));
	}

	@Test
	public void renamedFieldSilentlyBecomesNull() {

		// A rename is a removal plus an addition, so it is equally silent.
		DeployedDto dto = parse("{'uuid':'A1','ageGroup':'AGE_4_23M','populationCount':120}");

		assertThat(dto.population, is(nullValue()));
	}

	@Test
	public void unknownEnumConstantSilentlyBecomesNull() {

		// This is the shape of issue #975 (new IPV round value) and issue #986 (renaming a population
		// category constant). Gson maps an unrecognised constant to null instead of raising an error, so a
		// deployed apk quietly loses the field rather than reporting a problem.
		DeployedDto dto = parse("{'uuid':'A1','ageGroup':'AGE_4_59M','population':120}");

		assertThat(dto.ageGroup, is(nullValue()));
		assertThat(dto.population, is(120));
	}

	@Test
	public void incompatibleTypeChangeFailsLoudly() {

		// Type changes are the one category that surfaces as an error rather than silently.
		try {
			parse("{'uuid':'A1','population':'many'}");
			fail("expected a JsonSyntaxException for an incompatible type change");
		} catch (JsonSyntaxException expected) {
			// expected
		}
	}

	@Test
	public void numericStringIsCoercedSoTypeChangesAreNotReliablyLoud() {

		// Gson coerces a numeric string into an int, so "the type changed" does not dependably produce an
		// error either. Type changes cannot be treated as a safely self-reporting category.
		DeployedDto dto = parse("{'uuid':'A1','population':'120'}");

		assertThat(dto.population, is(120));
	}

	@Test
	public void reportDateMustRemainEpochMillis() {

		// RetroProvider registers a Date adapter that calls getAsLong() with no guard, so the server
		// switching to an ISO-8601 representation would break date parsing on every deployed apk.
		try {
			parse("{'uuid':'A1','reportDate':'2023-11-14T22:13:20Z'}");
			fail("expected date parsing to fail when the wire format is not epoch millis");
		} catch (RuntimeException expected) {
			// expected
		}
	}
}
