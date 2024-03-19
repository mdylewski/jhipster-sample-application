package com.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myapp.IntegrationTest;
import com.myapp.domain.A;
import com.myapp.repository.ARepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AResourceIT {

    private static final String DEFAULT_DEPARURE = "AAAAAAAAAA";
    private static final String UPDATED_DEPARURE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/as";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ARepository aRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAMockMvc;

    private A a;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static A createEntity(EntityManager em) {
        A a = new A().deparure(DEFAULT_DEPARURE);
        return a;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static A createUpdatedEntity(EntityManager em) {
        A a = new A().deparure(UPDATED_DEPARURE);
        return a;
    }

    @BeforeEach
    public void initTest() {
        a = createEntity(em);
    }

    @Test
    @Transactional
    void createA() throws Exception {
        int databaseSizeBeforeCreate = aRepository.findAll().size();
        // Create the A
        restAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(a)))
            .andExpect(status().isCreated());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeCreate + 1);
        A testA = aList.get(aList.size() - 1);
        assertThat(testA.getDeparure()).isEqualTo(DEFAULT_DEPARURE);
    }

    @Test
    @Transactional
    void createAWithExistingId() throws Exception {
        // Create the A with an existing ID
        a.setId(1L);

        int databaseSizeBeforeCreate = aRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(a)))
            .andExpect(status().isBadRequest());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDeparureIsRequired() throws Exception {
        int databaseSizeBeforeTest = aRepository.findAll().size();
        // set the field null
        a.setDeparure(null);

        // Create the A, which fails.

        restAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(a)))
            .andExpect(status().isBadRequest());

        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAS() throws Exception {
        // Initialize the database
        aRepository.saveAndFlush(a);

        // Get all the aList
        restAMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(a.getId().intValue())))
            .andExpect(jsonPath("$.[*].deparure").value(hasItem(DEFAULT_DEPARURE)));
    }

    @Test
    @Transactional
    void getA() throws Exception {
        // Initialize the database
        aRepository.saveAndFlush(a);

        // Get the a
        restAMockMvc
            .perform(get(ENTITY_API_URL_ID, a.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(a.getId().intValue()))
            .andExpect(jsonPath("$.deparure").value(DEFAULT_DEPARURE));
    }

    @Test
    @Transactional
    void getNonExistingA() throws Exception {
        // Get the a
        restAMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingA() throws Exception {
        // Initialize the database
        aRepository.saveAndFlush(a);

        int databaseSizeBeforeUpdate = aRepository.findAll().size();

        // Update the a
        A updatedA = aRepository.findById(a.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedA are not directly saved in db
        em.detach(updatedA);
        updatedA.deparure(UPDATED_DEPARURE);

        restAMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedA.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(updatedA))
            )
            .andExpect(status().isOk());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
        A testA = aList.get(aList.size() - 1);
        assertThat(testA.getDeparure()).isEqualTo(UPDATED_DEPARURE);
    }

    @Test
    @Transactional
    void putNonExistingA() throws Exception {
        int databaseSizeBeforeUpdate = aRepository.findAll().size();
        a.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAMockMvc
            .perform(
                put(ENTITY_API_URL_ID, a.getId()).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(a))
            )
            .andExpect(status().isBadRequest());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchA() throws Exception {
        int databaseSizeBeforeUpdate = aRepository.findAll().size();
        a.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(a))
            )
            .andExpect(status().isBadRequest());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamA() throws Exception {
        int databaseSizeBeforeUpdate = aRepository.findAll().size();
        a.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(a)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAWithPatch() throws Exception {
        // Initialize the database
        aRepository.saveAndFlush(a);

        int databaseSizeBeforeUpdate = aRepository.findAll().size();

        // Update the a using partial update
        A partialUpdatedA = new A();
        partialUpdatedA.setId(a.getId());

        partialUpdatedA.deparure(UPDATED_DEPARURE);

        restAMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedA.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedA))
            )
            .andExpect(status().isOk());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
        A testA = aList.get(aList.size() - 1);
        assertThat(testA.getDeparure()).isEqualTo(UPDATED_DEPARURE);
    }

    @Test
    @Transactional
    void fullUpdateAWithPatch() throws Exception {
        // Initialize the database
        aRepository.saveAndFlush(a);

        int databaseSizeBeforeUpdate = aRepository.findAll().size();

        // Update the a using partial update
        A partialUpdatedA = new A();
        partialUpdatedA.setId(a.getId());

        partialUpdatedA.deparure(UPDATED_DEPARURE);

        restAMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedA.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedA))
            )
            .andExpect(status().isOk());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
        A testA = aList.get(aList.size() - 1);
        assertThat(testA.getDeparure()).isEqualTo(UPDATED_DEPARURE);
    }

    @Test
    @Transactional
    void patchNonExistingA() throws Exception {
        int databaseSizeBeforeUpdate = aRepository.findAll().size();
        a.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, a.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(a))
            )
            .andExpect(status().isBadRequest());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchA() throws Exception {
        int databaseSizeBeforeUpdate = aRepository.findAll().size();
        a.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(a))
            )
            .andExpect(status().isBadRequest());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamA() throws Exception {
        int databaseSizeBeforeUpdate = aRepository.findAll().size();
        a.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(a)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the A in the database
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteA() throws Exception {
        // Initialize the database
        aRepository.saveAndFlush(a);

        int databaseSizeBeforeDelete = aRepository.findAll().size();

        // Delete the a
        restAMockMvc.perform(delete(ENTITY_API_URL_ID, a.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<A> aList = aRepository.findAll();
        assertThat(aList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
