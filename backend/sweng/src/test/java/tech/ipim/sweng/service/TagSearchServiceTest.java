package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tech.ipim.sweng.dto.TagDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.Tag;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.TagRepository;
import tech.ipim.sweng.repository.UserRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TagSearchServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TagSearchService tagSearchService;

    private User testUser;
    private Tag tag1;
    private Tag tag2;
    private Tag tag3;
    private Note testNote;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        tag1 = new Tag("importante");
        tag1.setId(1L);

        tag2 = new Tag("lavoro");
        tag2.setId(2L);

        tag3 = new Tag("studio");
        tag3.setId(3L);

        testNote = new Note("Test Note", "Test content", testUser);
        testNote.setId(1L);
    }

    @Test
    void shouldSearchTagsByName() {
        // Given
        when(tagRepository.findByNomeContainingIgnoreCase("import")).thenReturn(Arrays.asList(tag1));

        // When
        List<TagDto> result = tagSearchService.searchTagsByName("import");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("importante");
        verify(tagRepository).findByNomeContainingIgnoreCase("import");
    }

    @Test
    void shouldReturnEmptyListWhenNoTagsFoundByName() {
        // Given
        when(tagRepository.findByNomeContainingIgnoreCase("inesistente")).thenReturn(Collections.emptyList());

        // When
        List<TagDto> result = tagSearchService.searchTagsByName("inesistente");

        // Then
        assertThat(result).isEmpty();
        verify(tagRepository).findByNomeContainingIgnoreCase("inesistente");
    }

    @Test
    void shouldThrowExceptionWhenSearchingWithNullTagName() {
        // When & Then
        assertThatThrownBy(() -> tagSearchService.searchTagsByName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Il nome del tag non può essere nullo");

        verify(tagRepository, never()).findByNomeContainingIgnoreCase(anyString());
    }

    @Test
    void shouldThrowExceptionWhenSearchingWithEmptyTagName() {
        // When & Then
        assertThatThrownBy(() -> tagSearchService.searchTagsByName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Il nome del tag non può essere vuoto");

        verify(tagRepository, never()).findByNomeContainingIgnoreCase(anyString());
    }

    @Test
    void shouldGetAllTags() {
        // Given
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2, tag3));

        // When
        List<TagDto> result = tagSearchService.getAllTags();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(TagDto::getNome)
                .containsExactly("importante", "lavoro", "studio");
        verify(tagRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoTagsExist() {
        // Given
        when(tagRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<TagDto> result = tagSearchService.getAllTags();

        // Then
        assertThat(result).isEmpty();
        verify(tagRepository).findAll();
    }

    @Test
    void shouldGetTagsByUsage() {
        // Given
        when(tagRepository.findTagsByUsageCount()).thenReturn(Arrays.asList(tag1, tag2, tag3));

        // When
        List<TagDto> result = tagSearchService.getTagsByUsage();

        // Then
        assertThat(result).hasSize(3);
        verify(tagRepository).findTagsByUsageCount();
    }

    @Test
    void shouldGetPopularTags() {
        // Given
        when(tagRepository.findMostPopularTags(5)).thenReturn(Arrays.asList(tag1, tag2));

        // When
        List<TagDto> result = tagSearchService.getPopularTags(5);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNome()).isEqualTo("importante");
        assertThat(result.get(1).getNome()).isEqualTo("lavoro");
        verify(tagRepository).findMostPopularTags(5);
    }

    @Test
    void shouldThrowExceptionWhenGettingPopularTagsWithNegativeLimit() {
        // When & Then
        assertThatThrownBy(() -> tagSearchService.getPopularTags(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Il limite deve essere positivo");

        verify(tagRepository, never()).findMostPopularTags(anyInt());
    }

    @Test
    void shouldGetTagsUsedByUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tagRepository.findTagsByUser(testUser)).thenReturn(Arrays.asList(tag1, tag3));

        // When
        List<TagDto> result = tagSearchService.getTagsUsedByUser("testuser");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TagDto::getNome)
                .containsExactly("importante", "studio");
        verify(userRepository).findByUsername("testuser");
        verify(tagRepository).findTagsByUser(testUser);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForTagsUsed() {
        // Given
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tagSearchService.getTagsUsedByUser("inesistente"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: inesistente");

        verify(userRepository).findByUsername("inesistente");
        verify(tagRepository, never()).findTagsByUser(any(User.class));
    }

    @Test
    void shouldGetUnusedTags() {
        // Given
        when(tagRepository.findUnusedTags()).thenReturn(Arrays.asList(tag2));

        // When
        List<TagDto> result = tagSearchService.getUnusedTags();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("lavoro");
        verify(tagRepository).findUnusedTags();
    }

    @Test
    void shouldGetTagsWithNoteCount() {
        // Given
        when(tagRepository.findTagsWithNoteCount()).thenReturn(Arrays.asList(
                new Object[]{"importante", 5L},
                new Object[]{"lavoro", 3L},
                new Object[]{"studio", 2L}
        ));

        // When
        List<TagDto> result = tagSearchService.getTagsWithNoteCount();

        // Then
        assertThat(result).hasSize(3);
        // Verifica che i tag siano ordinati per conteggio decrescente
        assertThat(result.get(0).getNome()).isEqualTo("importante");
        assertThat(result.get(1).getNome()).isEqualTo("lavoro");
        assertThat(result.get(2).getNome()).isEqualTo("studio");
        verify(tagRepository).findTagsWithNoteCount();
    }

    @Test
    void shouldGetRecentlyUsedTags() {
        // Given
        when(tagRepository.findRecentlyUsedTags(7)).thenReturn(Arrays.asList(tag1, tag3));

        // When
        List<TagDto> result = tagSearchService.getRecentlyUsedTags(7);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TagDto::getNome)
                .containsExactly("importante", "studio");
        verify(tagRepository).findRecentlyUsedTags(7);
    }

    @Test
    void shouldThrowExceptionWhenGettingRecentlyUsedTagsWithNegativeDays() {
        // When & Then
        assertThatThrownBy(() -> tagSearchService.getRecentlyUsedTags(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Il numero di giorni deve essere positivo");

        verify(tagRepository, never()).findRecentlyUsedTags(anyInt());
    }

    @Test
    void shouldGetTagsSimilarTo() {
        // Given
        when(tagRepository.findSimilarTags("lavoro")).thenReturn(Arrays.asList(tag2, tag3));

        // When
        List<TagDto> result = tagSearchService.getTagsSimilarTo("lavoro");

        // Then
        assertThat(result).hasSize(2);
        verify(tagRepository).findSimilarTags("lavoro");
    }

    @Test
    void shouldGetTagStatistics() {
        // Given
        when(tagRepository.count()).thenReturn(10L);
        when(tagRepository.countUsedTags()).thenReturn(8L);
        when(tagRepository.findMostUsedTag()).thenReturn(Optional.of(tag1));

        // When
        TagSearchService.TagStatistics result = tagSearchService.getTagStatistics();

        // Then
        assertThat(result.getTotalTags()).isEqualTo(10L);
        assertThat(result.getUsedTags()).isEqualTo(8L);
        assertThat(result.getUnusedTags()).isEqualTo(2L);
        assertThat(result.getMostUsedTag()).isEqualTo("importante");

        verify(tagRepository).count();
        verify(tagRepository).countUsedTags();
        verify(tagRepository).findMostUsedTag();
    }

    @Test
    void shouldGetTagStatisticsWithNoMostUsedTag() {
        // Given
        when(tagRepository.count()).thenReturn(0L);
        when(tagRepository.countUsedTags()).thenReturn(0L);
        when(tagRepository.findMostUsedTag()).thenReturn(Optional.empty());

        // When
        TagSearchService.TagStatistics result = tagSearchService.getTagStatistics();

        // Then
        assertThat(result.getTotalTags()).isEqualTo(0L);
        assertThat(result.getUsedTags()).isEqualTo(0L);
        assertThat(result.getUnusedTags()).isEqualTo(0L);
        assertThat(result.getMostUsedTag()).isNull();
    }

    @Test
    void shouldSearchTagsWithPrefix() {
        // Given
        when(tagRepository.findByNomeStartingWithIgnoreCase("imp")).thenReturn(Arrays.asList(tag1));

        // When
        List<TagDto> result = tagSearchService.searchTagsWithPrefix("imp");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("importante");
        verify(tagRepository).findByNomeStartingWithIgnoreCase("imp");
    }

    @Test
    void shouldGetTagsAlphabetically() {
        // Given
        when(tagRepository.findAllByOrderByNomeAsc()).thenReturn(Arrays.asList(tag1, tag2, tag3));

        // When
        List<TagDto> result = tagSearchService.getTagsAlphabetically();

        // Then
        assertThat(result).hasSize(3);
        // I tag dovrebbero essere già ordinati alfabeticamente dal repository
        assertThat(result).extracting(TagDto::getNome)
                .containsExactly("importante", "lavoro", "studio");
        verify(tagRepository).findAllByOrderByNomeAsc();
    }
}