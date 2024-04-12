import unittest
from caseprocessor.util.case_ref_generator import get_case_ref
class CaseGeneratorTest(unittest.TestCase):

    # Values taken from Java Test
    def test_case_ref_generator_single(self):
        case_ref_generator_key = b'\x10\x20\x10\x20\x10\x20\x10\x20'
        expected_case_ref = 2459403677
        python_result = get_case_ref(99, case_ref_generator_key)
        self.assertEqual(python_result, expected_case_ref)

    @unittest.skip("Takes a while to complete")
    def test_case_ref_generator(self):
        """
        TODO: discuss if it's worth trying to make this more efficient -> concurrency?
        This may take a couple of minutes, on the java one it's disabled by default
        """
        case_ref_generator_key = b'\x10\x20\x10\x20\x10\x20\x10\x20'
        max_num_of_caserefs_to_check = 89999998

        results = []
        print("--- Generating Case Refs ---")
        for i in range(max_num_of_caserefs_to_check):
            if i % 10000 == 0:
                print(f"{i/max_num_of_caserefs_to_check * 100}%")
            result = get_case_ref(i, case_ref_generator_key)
            self.assertTrue(9999999999 > result > 1000000000)
            results.append(result)

        results.sort()
        for i in range(max_num_of_caserefs_to_check -1):
            self.assertTrue(results[i] != results[i+1])

